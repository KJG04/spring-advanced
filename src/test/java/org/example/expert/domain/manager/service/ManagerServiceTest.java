package org.example.expert.domain.manager.service;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private ManagerService managerService;

    @Nested
    @DisplayName("ManagerService::getManagers()")
    class Class1 {
        @Test
        @DisplayName("manager 목록 조회 시 Todo가 없다면 InvalidRequestException 에러를 던진다")
        public void test1() {
            // given
            long todoId = 1L;
            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> managerService.getManagers(todoId));
            assertEquals("Manager not found", exception.getMessage());
        }

        @Test
        @DisplayName("manager 목록 조회에 성공한다")
        public void test2() {
            // given
            long todoId = 1L;
            User user = new User("user1@example.com", "password", UserRole.USER);
            Todo todo = new Todo("Title", "Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo, "id", todoId);

            Manager mockManager = new Manager(todo.getUser(), todo);
            List<Manager> managerList = List.of(mockManager);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

            // when
            List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

            // then
            assertEquals(1, managerResponses.size());
            assertEquals(mockManager.getId(), managerResponses.get(0).getId());
            assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
        }
    }

    @Nested
    @DisplayName("ManagerService::saveManager()")
    class Class2 {
        @Test
        @DisplayName("todo의 user가 null인 경우 예외가 발생한다")
        void test1() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            long todoId = 1L;
            long managerUserId = 2L;

            Todo todo = new Todo();
            ReflectionTestUtils.setField(todo, "user", null);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("user와 todo.user가 다르면 예외가 발생한다")
        void test1_2() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);

            User user2 = User.fromAuthUser(authUser);  // 일정을 만든 유저
            ReflectionTestUtils.setField(user2, "id", 2L);

            long todoId = 1L;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user2);

            long managerUserId = 2L;

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
                    managerService.saveManager(authUser, todoId, managerSaveRequest)
            );

            assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
        }


        @Test
        @DisplayName("todo가 정상적으로 등록된다")
        void test2() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            long todoId = 1L;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long managerUserId = 2L;
            User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
            given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

            // then
            assertNotNull(response);
            assertEquals(managerUser.getId(), response.getUser().getId());
            assertEquals(managerUser.getEmail(), response.getUser().getEmail());
        }

        @Test
        @DisplayName("일정 작성자는 본인을 담당자로 등록할 수 없습니다.")
        void test3() {
            //given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            long todoId = 1L;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long managerUserId = 1L;
            User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> managerService.saveManager(authUser, todoId, managerSaveRequest));
            assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", invalidRequestException.getMessage());
        }
    }

    @Nested
    @DisplayName("ManagerService::deleteManager()")
    class Class3 {
        @Test
        @DisplayName("user가 없으면 InvalidRequestException 예외가 발생한다")
        void test1() {
            // given
            long userId = 1;
            long todoId = 1;
            long managerId = 1;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> managerService.deleteManager(userId, todoId, managerId));
            assertEquals("User not found", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("todo가 없으면 InvalidRequestException 예외가 발생한다")
        void test2() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

            long todoId = 1;
            long managerId = 1;
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> managerService.deleteManager(user.getId(), todoId, managerId));
            assertEquals("Todo not found", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("todo.getUser()가 없으면 InvalidRequestException 예외가 발생한다")
        void test3() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);  // 일정을 만든 유저
            long todoId = 1;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", null);
            long managerId = 1;
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> managerService.deleteManager(user.getId(), todoId, managerId));
            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("todo user와 user가 다르면 InvalidRequestException 예외가 발생한다")
        void test4() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);

            long managerUserId = 2L;
            User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
            ReflectionTestUtils.setField(managerUser, "id", managerUserId);

            long todoId = 1;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", managerUser);
            long managerId = 1;
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> managerService.deleteManager(user.getId(), todoId, managerId));
            assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("manager가 없으면 InvalidRequestException 예외가 발생한다")
        void test5() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);

            long todoId = 1;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long managerId = 1;

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findById(managerId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> managerService.deleteManager(user.getId(), todoId, managerId));
            assertEquals("Manager not found", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("manager의 todo와 todo다 다르면 InvalidRequestException 예외가 발생한다")
        void test6() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);

            long todoId = 1;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long todo2Id = 2;
            Todo todo2 = new Todo("Test Title", "Test Contents", "Sunny", user);
            ReflectionTestUtils.setField(todo2, "id", todo2Id);

            long managerId = 1;
            Manager manager = new Manager(user, todo2);

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> managerService.deleteManager(user.getId(), todoId, managerId));
            assertEquals("해당 일정에 등록된 담당자가 아닙니다.", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("manager가 제대로 삭제된다.")
        void test7() {
            // given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);

            long todoId = 1;
            Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

            long managerId = 1;
            Manager manager = new Manager(user, todo);

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
            given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

            // when
            managerService.deleteManager(user.getId(), todoId, managerId);

            // then
            verify(managerRepository, times(1)).delete(manager);
        }
    }
}
