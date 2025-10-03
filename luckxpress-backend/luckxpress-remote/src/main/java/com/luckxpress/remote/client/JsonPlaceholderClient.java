package com.luckxpress.remote.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Feign client for JSONPlaceholder API
 * Example external API integration
 */
@FeignClient(
    name = "jsonplaceholder",
    url = "${external.api.jsonplaceholder.url:https://jsonplaceholder.typicode.com}",
    configuration = FeignClientConfig.class
)
public interface JsonPlaceholderClient {

    /**
     * Get all posts
     * @return list of posts
     */
    @GetMapping("/posts")
    List<Post> getAllPosts();

    /**
     * Get post by ID
     * @param id post ID
     * @return post details
     */
    @GetMapping("/posts/{id}")
    Post getPostById(@PathVariable("id") Long id);

    /**
     * Create a new post
     * @param post post data
     * @return created post
     */
    @PostMapping("/posts")
    Post createPost(@RequestBody Post post);

    /**
     * Update a post
     * @param id post ID
     * @param post updated post data
     * @return updated post
     */
    @PutMapping("/posts/{id}")
    Post updatePost(@PathVariable("id") Long id, @RequestBody Post post);

    /**
     * Delete a post
     * @param id post ID
     */
    @DeleteMapping("/posts/{id}")
    void deletePost(@PathVariable("id") Long id);

    /**
     * Get comments for a post
     * @param postId post ID
     * @return list of comments
     */
    @GetMapping("/posts/{postId}/comments")
    List<Comment> getPostComments(@PathVariable("postId") Long postId);

    /**
     * Get all users
     * @return list of users
     */
    @GetMapping("/users")
    List<Map<String, Object>> getAllUsers();

    /**
     * Post DTO
     */
    class Post {
        private Long id;
        private Long userId;
        private String title;
        private String body;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    /**
     * Comment DTO
     */
    class Comment {
        private Long id;
        private Long postId;
        private String name;
        private String email;
        private String body;

        // Getters and setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getPostId() {
            return postId;
        }

        public void setPostId(Long postId) {
            this.postId = postId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }
}
