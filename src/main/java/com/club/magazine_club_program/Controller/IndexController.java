package com.club.magazine_club_program.Controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
public class IndexController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> home() {
        String html = """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Sensitibook API Server</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .container {
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                        max-width: 800px;
                        width: 100%;
                        padding: 40px;
                    }
                    h1 {
                        color: #333;
                        margin-bottom: 10px;
                        font-size: 2.5em;
                    }
                    .status {
                        display: inline-block;
                        background: #10b981;
                        color: white;
                        padding: 5px 15px;
                        border-radius: 20px;
                        font-size: 0.9em;
                        margin-bottom: 30px;
                    }
                    .section {
                        margin-top: 30px;
                    }
                    .section-title {
                        font-size: 1.3em;
                        color: #555;
                        margin-bottom: 15px;
                        border-bottom: 2px solid #667eea;
                        padding-bottom: 10px;
                    }
                    .endpoints {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                        gap: 15px;
                        margin-top: 20px;
                    }
                    .endpoint {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 10px;
                        border-left: 4px solid #667eea;
                        transition: transform 0.2s, box-shadow 0.2s;
                    }
                    .endpoint:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 5px 15px rgba(0,0,0,0.1);
                    }
                    .endpoint-name {
                        font-weight: bold;
                        color: #333;
                        margin-bottom: 5px;
                    }
                    .endpoint-path {
                        color: #667eea;
                        font-family: 'Courier New', monospace;
                        font-size: 0.9em;
                    }
                    .json-link {
                        margin-top: 30px;
                        text-align: center;
                        padding-top: 20px;
                        border-top: 1px solid #e0e0e0;
                    }
                    .json-link a {
                        color: #667eea;
                        text-decoration: none;
                        font-weight: bold;
                    }
                    .json-link a:hover {
                        text-decoration: underline;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>📚 Sensitibook</h1>
                    <span class="status">✅ Server Running</span>
                    <p style="color: #666; margin-top: 10px;">API 서버가 정상적으로 실행 중입니다.</p>
                    
                    <div class="section">
                        <div class="section-title">🔗 API Endpoints</div>
                        <div class="endpoints">
                            <div class="endpoint">
                                <div class="endpoint-name">Members</div>
                                <div class="endpoint-path">/members</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Management</div>
                                <div class="endpoint-path">/management</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Admin Auth</div>
                                <div class="endpoint-path">/admin/auth</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Admin Ban</div>
                                <div class="endpoint-path">/admin/ban</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Admin Chat</div>
                                <div class="endpoint-path">/admin/chat</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Club Posts</div>
                                <div class="endpoint-path">/club-posts</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Assignments</div>
                                <div class="endpoint-path">/assignments</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Notifications</div>
                                <div class="endpoint-path">/notifications</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Instagram</div>
                                <div class="endpoint-path">/instagram</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">Photos</div>
                                <div class="endpoint-path">/photos</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">OAuth2 Token</div>
                                <div class="endpoint-path">/oauth2/token</div>
                            </div>
                            <div class="endpoint">
                                <div class="endpoint-name">OAuth2 Login</div>
                                <div class="endpoint-path">/login/oauth2</div>
                            </div>
                        </div>
                    </div>
                    
                    <div class="json-link">
                        <a href="/api/info">JSON 형식으로 보기</a>
                    </div>
                </div>
            </body>
            </html>
            """;
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping("/api/info")
    public ResponseEntity<Map<String, Object>> apiInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensitibook API Server");
        response.put("status", "running");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("members", "/members");
        endpoints.put("management", "/management");
        endpoints.put("admin_auth", "/admin/auth");
        endpoints.put("admin_ban", "/admin/ban");
        endpoints.put("admin_chat", "/admin/chat");
        endpoints.put("club_posts", "/club-posts");
        endpoints.put("assignments", "/assignments");
        endpoints.put("notifications", "/notifications");
        endpoints.put("instagram", "/instagram");
        endpoints.put("photos", "/photos");
        endpoints.put("oauth2_token", "/oauth2/token");
        endpoints.put("login_oauth2", "/login/oauth2");
        
        response.put("endpoints", endpoints);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }
}