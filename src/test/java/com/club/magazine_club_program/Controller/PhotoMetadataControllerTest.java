package com.club.magazine_club_program.Controller;

import com.club.magazine_club_program.Service.PhotoMetadataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotoMetadataController.class)
public class PhotoMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PhotoMetadataService service;

    @Test
    public void upload_returnsOk() throws Exception {
        when(service.extractMetadata(any())).thenReturn(null);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{});

        mockMvc.perform(multipart("/photos/metadata").file(file))
                .andExpect(status().isOk());
    }
}


