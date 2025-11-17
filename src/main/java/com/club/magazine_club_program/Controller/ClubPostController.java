package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.DTO.ClubPostDTO;
import com.club.magazine_club_program.Service.ClubPostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/club-posts")
public class ClubPostController {

    private final ClubPostService clubPostService;

    public ClubPostController(ClubPostService clubPostService) {
        this.clubPostService = clubPostService;
    }

    // 모든 활성화된 모임 글 조회
    @GetMapping
    public ResponseEntity<?> getAllActivePosts() {
        try {
            List<ClubPostDTO> posts = clubPostService.getAllActivePosts();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.ok("모임 글 조회 실패: " + e.getMessage());
        }
    }

    // 모든 모임 글 조회 (관리자용)
    @GetMapping("/all")
    public ResponseEntity<?> getAllPosts() {
        try {
            List<ClubPostDTO> posts = clubPostService.getAllPosts();
            return ResponseEntity.ok(posts);
        } catch (Exception e) {
            return ResponseEntity.ok("모임 글 조회 실패: " + e.getMessage());
        }
    }

    // ID로 모임 글 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Integer id) {
        try {
            ClubPostDTO post = clubPostService.getPostById(id);
            if (post == null) {
                return ResponseEntity.ok("모임 글을 찾을 수 없습니다.");
            }
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.ok("모임 글 조회 실패: " + e.getMessage());
        }
    }

    // 모임 글 생성
    @PostMapping
    public ResponseEntity<?> createPost(@RequestBody ClubPostDTO clubPost) {
        try {
            ClubPostDTO created = clubPostService.createPost(clubPost);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.ok("모임 글 생성 실패: " + e.getMessage());
        }
    }

    // 모임 글 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Integer id, @RequestBody ClubPostDTO clubPost) {
        try {
            clubPost.setId(id);
            boolean updated = clubPostService.updatePost(clubPost);
            if (updated) {
                return ResponseEntity.ok("모임 글 수정 완료");
            } else {
                return ResponseEntity.ok("모임 글 수정 실패");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("모임 글 수정 실패: " + e.getMessage());
        }
    }

    // 모임 글 삭제 (비활성화)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Integer id) {
        try {
            boolean deleted = clubPostService.deletePost(id);
            if (deleted) {
                return ResponseEntity.ok("모임 글 삭제 완료");
            } else {
                return ResponseEntity.ok("모임 글 삭제 실패");
            }
        } catch (Exception e) {
            return ResponseEntity.ok("모임 글 삭제 실패: " + e.getMessage());
        }
    }
}

