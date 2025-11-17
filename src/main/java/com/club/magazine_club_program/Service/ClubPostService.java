package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.ClubPostDTO;
import com.club.magazine_club_program.Mapper.ClubPostMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

