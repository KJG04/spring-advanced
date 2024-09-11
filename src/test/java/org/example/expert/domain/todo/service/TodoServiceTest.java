package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {
    @Mock
    TodoRepository todoRepository;

    @Mock
    WeatherClient weatherClient;

    @InjectMocks
    TodoService todoService;

    @Nested
    @DisplayName("TodoService::saveTodo()")
    class Class1 {
        @Test
        @DisplayName("todo가 정상적으로 저장된다.")
        void test1() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            TodoSaveRequest todoSaveRequest = new TodoSaveRequest("제목", "콘텐츠");

            given(weatherClient.getTodayWeather()).willReturn("Sunny");
            given(todoRepository.save(any())).willAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

            // when
            TodoSaveResponse todoSaveResponse = todoService.saveTodo(authUser, todoSaveRequest);

            // then
            assertEquals(todoSaveResponse.getTitle(), "제목");
            assertEquals(todoSaveResponse.getContents(), "콘텐츠");
            assertEquals(todoSaveResponse.getWeather(), "Sunny");
            assertNotNull(todoSaveResponse.getUser());
            assertEquals(todoSaveResponse.getUser().getId(), authUser.getId());
            assertEquals(todoSaveResponse.getUser().getEmail(), authUser.getEmail());
        }
    }

    @Nested
    @DisplayName("TodoService::getTodos()")
    class Class2 {
        @Test
        @DisplayName("todo 목록이 잘 불러와진다.")
        void test1() {
            //given
            int page = 1;
            int size = 5;
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);

            ArrayList<Todo> todoList = new ArrayList<>(List.of());
            for (int i = 0; i < size; i++) {
                todoList.add(new Todo("제목" + i, "콘텐츠" + i, "Sunny", user));
            }
            Page<Todo> todoPage = new PageImpl<>(todoList);

            given(todoRepository.findAllByOrderByModifiedAtDesc(any())).willReturn(todoPage);

            // when
            Page<TodoResponse> todoResponsePage = todoService.getTodos(page, size);

            // then
            for (int i = 0; i < todoResponsePage.getSize(); i++) {
                TodoResponse todoResponse = todoResponsePage.toList().get(i);
                Todo todo = todoList.get(i);
                assertEquals(todoResponse.getTitle(), todo.getTitle());
                assertEquals(todoResponse.getContents(), todo.getContents());
                assertEquals(todoResponse.getWeather(), todo.getWeather());
                assertNotNull(todoResponse.getUser());
                assertEquals(todoResponse.getUser().getId(), user.getId());
                assertEquals(todoResponse.getUser().getEmail(), user.getEmail());
            }
        }
    }

    @Nested
    @DisplayName("TodoService::getTodo()")
    class Class3 {
        @Test
        @DisplayName("todo가 없으면 예외가 발생한다.")
        void test1() {
            //given
            given(todoRepository.findByIdWithUser(any())).willReturn(Optional.empty());

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> todoService.getTodo(1L));
            assertEquals(invalidRequestException.getMessage(), "Todo not found");
        }

        @Test
        @DisplayName("todo가 정상적으로 가져와진다.")
        void test2() {
            //given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            Long todoId = 1L;
            Todo todo = new Todo("제목", "컨텐츠", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);
            given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.of(todo));

            // when
            TodoResponse todoResponse = todoService.getTodo(todoId);

            // then
            assertEquals(todoResponse.getTitle(), todo.getTitle());
            assertEquals(todoResponse.getContents(), todo.getContents());
            assertEquals(todoResponse.getWeather(), todo.getWeather());
            assertEquals(todoResponse.getUser().getId(), todo.getUser().getId());
            assertEquals(todoResponse.getUser().getEmail(), todo.getUser().getEmail());
        }
    }
}
