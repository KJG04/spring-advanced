package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CommentAdminServiceTest {
    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    CommentAdminService commentAdminService;

    @Nested
    @DisplayName("CommentAdminService::deleteComment()")
    class Class1 {

        @Test
        @DisplayName("삭제가 정상적으로 작동한다.")
        void test1() {
            // given
            Long commentId = 1L;

            // when
            commentAdminService.deleteComment(commentId);

            // then
            verify(commentRepository, times(1)).deleteById(commentId);
        }
    }
}
