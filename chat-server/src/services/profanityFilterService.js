const fs = require('fs').promises;
const path = require('path');
const pool = require('../db/pool');

/**
 * 금칙어 필터링 서비스
 * - 파일 기반 금칙어 관리
 * - 파일 변경 즉시 감지하여 DB 동기화
 * - 메시지에서 금칙어 검사
 * - 금칙어 발견 시 자동 차단 처리
 */
class ProfanityFilterService {
  constructor() {
    this.profanityWords = new Set(); // 메모리 캐시
    this.wordsFilePath = path.join(__dirname, '../../profanity-words.txt');
    this.lastFileModified = null;
    this.syncInProgress = false; // 동기화 중복 실행 방지
    
    // 초기 로드 및 DB 동기화
    this.initialize();
    
    // 파일 변경 감지 (1분마다 체크)
    setInterval(() => this.checkFileAndSync(), 60 * 1000);
    
    // 파일 시스템 감시 (선택사항, 더 빠른 반응)
    this.watchFile();
  }

  /**
   * 초기화: 파일에서 금칙어 로드 및 DB 동기화
   */
  async initialize() {
    try {
      await this.syncFromFile();
      console.log(`✅ 금칙어 필터링 서비스 초기화 완료: ${this.profanityWords.size}개`);
    } catch (error) {
      console.error('❌ 금칙어 필터링 서비스 초기화 실패:', error);
      // DB에서 읽기 시도 (fallback)
      await this.loadFromDatabase();
    }
  }

  /**
   * 파일 변경 감지 및 동기화
   */
  async checkFileAndSync() {
    try {
      const stats = await fs.stat(this.wordsFilePath);
      const modifiedTime = stats.mtime.getTime();
      
      // 파일이 변경되었으면 동기화
      if (this.lastFileModified !== modifiedTime) {
        await this.syncFromFile();
      }
    } catch (error) {
      if (error.code !== 'ENOENT') {
        console.error('❌ 파일 변경 감지 실패:', error);
      }
    }
  }

  /**
   * 파일 시스템 감시 (더 빠른 반응)
   */
  watchFile() {
    try {
      const fsWatch = require('fs');
      fsWatch.watchFile(this.wordsFilePath, { interval: 5000 }, async (curr, prev) => {
        if (curr.mtime !== prev.mtime) {
          console.log('📝 금칙어 파일 변경 감지됨');
          await this.syncFromFile();
        }
      });
      console.log('👀 금칙어 파일 감시 시작');
    } catch (error) {
      console.warn('⚠️ 파일 감시 설정 실패, 주기적 체크로 대체:', error.message);
    }
  }

  /**
   * 파일에서 금칙어 목록 읽기
   */
  async readWordsFromFile() {
    try {
      const content = await fs.readFile(this.wordsFilePath, 'utf-8');
      const stats = await fs.stat(this.wordsFilePath);
      
      const words = content
        .split('\n')
        .map(line => line.trim())
        .filter(line => line && !line.startsWith('#')); // 빈 줄과 주석 제거
      
      return {
        words,
        modifiedTime: stats.mtime.getTime(),
      };
    } catch (error) {
      if (error.code === 'ENOENT') {
        console.warn(`⚠️ 금칙어 파일이 없습니다: ${this.wordsFilePath}`);
        return { words: [], modifiedTime: null };
      }
      throw error;
    }
  }

  /**
   * 파일에서 금칙어 로드 및 DB 동기화
   */
  async syncFromFile() {
    // 동기화 중복 실행 방지
    if (this.syncInProgress) {
      return;
    }
    
    this.syncInProgress = true;
    
    try {
      const { words, modifiedTime } = await this.readWordsFromFile();
      
      // 파일이 변경되지 않았으면 스킵
      if (this.lastFileModified === modifiedTime && this.profanityWords.size > 0) {
        this.syncInProgress = false;
        return;
      }
      
      // 메모리 캐시 업데이트
      this.profanityWords = new Set(words.map(word => word.toLowerCase()));
      this.lastFileModified = modifiedTime;
      
      // DB 동기화: 파일의 금칙어를 DB에 반영
      await this.syncToDatabase(words);
      
      console.log(`✅ 금칙어 목록 동기화 완료: ${this.profanityWords.size}개 (파일 → DB)`);
    } catch (error) {
      console.error('❌ 금칙어 목록 동기화 실패:', error);
      // DB에서 읽기 시도 (fallback)
      await this.loadFromDatabase();
    } finally {
      this.syncInProgress = false;
    }
  }

  /**
   * 파일의 금칙어 목록을 DB에 동기화
   */
  async syncToDatabase(fileWords) {
    try {
      // DB의 모든 활성 금칙어 조회
      const [dbRows] = await pool.execute(
        'SELECT word FROM ProfanityWord WHERE is_active = TRUE'
      );
      const dbWords = new Set(dbRows.map(row => row.word.toLowerCase()));
      
      // 파일의 금칙어를 Set으로 변환
      const fileWordsSet = new Set(fileWords.map(word => word.toLowerCase()));
      
      // 파일에 있지만 DB에 없는 금칙어 추가
      for (const word of fileWordsSet) {
        if (!dbWords.has(word)) {
          await pool.execute(
            'INSERT INTO ProfanityWord (word, is_active) VALUES (?, TRUE) ON DUPLICATE KEY UPDATE is_active = TRUE',
            [word]
          );
          console.log(`➕ 금칙어 추가: ${word}`);
        }
      }
      
      // DB에 있지만 파일에 없는 금칙어 비활성화
      for (const word of dbWords) {
        if (!fileWordsSet.has(word)) {
          await pool.execute(
            'UPDATE ProfanityWord SET is_active = FALSE WHERE word = ?',
            [word]
          );
          console.log(`➖ 금칙어 비활성화: ${word}`);
        }
      }
    } catch (error) {
      console.error('❌ DB 동기화 실패:', error);
      throw error;
    }
  }

  /**
   * DB에서 금칙어 목록 로드 (fallback용)
   */
  async loadFromDatabase() {
    try {
      const [rows] = await pool.execute(
        'SELECT word FROM ProfanityWord WHERE is_active = TRUE'
      );
      this.profanityWords = new Set(rows.map(row => row.word.toLowerCase()));
      console.log(`✅ 금칙어 목록 DB에서 로드 완료: ${this.profanityWords.size}개 (fallback)`);
    } catch (error) {
      console.error('❌ 금칙어 목록 DB 로드 실패:', error);
      // 기본 금칙어 목록 (fallback)
      this.profanityWords = new Set();
    }
  }

  /**
   * 메시지에서 금칙어 검사
   * @param {string} message - 검사할 메시지
   * @returns {Object} { hasProfanity: boolean, detectedWords: string[] }
   */
  checkProfanity(message) {
    if (!message || typeof message !== 'string') {
      return { hasProfanity: false, detectedWords: [] };
    }

    const lowerMessage = message.toLowerCase();
    const detectedWords = [];

    // 금칙어 목록과 비교
    for (const word of this.profanityWords) {
      if (lowerMessage.includes(word)) {
        detectedWords.push(word);
      }
    }

    return {
      hasProfanity: detectedWords.length > 0,
      detectedWords,
    };
  }

  /**
   * 메시지에서 금칙어를 마스킹 처리
   * @param {string} message - 원본 메시지
   * @returns {string} 마스킹된 메시지
   */
  maskProfanity(message) {
    if (!message || typeof message !== 'string') {
      return message;
    }

    let maskedMessage = message;
    const lowerMessage = message.toLowerCase();

    for (const word of this.profanityWords) {
      if (lowerMessage.includes(word)) {
        const regex = new RegExp(word, 'gi');
        maskedMessage = maskedMessage.replace(regex, '*'.repeat(word.length));
      }
    }

    return maskedMessage;
  }

  /**
   * 현재 금칙어 목록 조회 (DB에서 읽은 내용)
   */
  getWords() {
    return Array.from(this.profanityWords).sort();
  }

}

// 싱글톤 인스턴스
const profanityFilterService = new ProfanityFilterService();

module.exports = profanityFilterService;

