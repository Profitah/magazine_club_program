package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.ClubPostDTO;
import com.club.magazine_club_program.Mapper.ClubPostMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ClubPostService {
    
    private final ClubPostMapper clubPostMapper;

    public ClubPostService(ClubPostMapper clubPostMapper) {
        this.clubPostMapper = clubPostMapper;
    }

    // 모든 활성화된 모임 글 조회
    public List<ClubPostDTO> getAllActivePosts() {
        return clubPostMapper.findAllActive();
    }

    // 모든 모임 글 조회 (관리자용)
    public List<ClubPostDTO> getAllPosts() {
        return clubPostMapper.findAll();
    }

    // ID로 모임 글 조회
    public ClubPostDTO getPostById(Integer id) {
        return clubPostMapper.findById(id);
    }

    // 모임 글 생성
    public ClubPostDTO createPost(ClubPostDTO clubPost) {
        if (clubPost.getCreatedAt() == null) {
            clubPost.setCreatedAt(LocalDateTime.now());
        }
        if (clubPost.getCreatedByAdminId() == null) {
            throw new IllegalArgumentException("작성자 Admin ID가 필요합니다.");
        }
        
        // 마감일 자동 설정: 생성일부터 7일 뒤
        if (clubPost.getDueDate() == null) {
            LocalDateTime dueDate = clubPost.getCreatedAt().plus(7, ChronoUnit.DAYS);
            // 마감일은 해당 날짜의 23:59:59로 설정
            clubPost.setDueDate(dueDate.toLocalDate().atTime(23, 59, 59));
        }
        
        clubPost.setActive(true);
        
        clubPostMapper.insert(clubPost);
        return clubPost;
    }

    // 모임 글 수정
    public boolean updatePost(ClubPostDTO clubPost) {
        int updatedRows = clubPostMapper.update(clubPost);
        return updatedRows > 0;
    }

    // 모임 글 삭제 (비활성화)
    public boolean deletePost(Integer id) {
        int updatedRows = clubPostMapper.delete(id);
        return updatedRows > 0;
    }
}

