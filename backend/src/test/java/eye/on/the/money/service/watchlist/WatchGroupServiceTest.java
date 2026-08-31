package eye.on.the.money.service.watchlist;

import eye.on.the.money.dto.in.WatchGroupEditDTO;
import eye.on.the.money.dto.out.WatchGroupDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.User;
import eye.on.the.money.model.watchlist.WatchGroup;
import eye.on.the.money.repository.watchlist.WatchGroupRepository;
import eye.on.the.money.service.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class WatchGroupServiceTest {

    private static final Long USER_ID = 42L;

    @Mock
    private WatchGroupRepository watchGroupRepository;
    @Mock
    private UserService userService;
    @InjectMocks
    private WatchGroupService watchGroupService;

    private final User user = User.builder().id(USER_ID).email("groups@example.test").build();

    private WatchGroup group(Long id, String name) {
        return WatchGroup.builder().id(id).name(name).user(this.user).build();
    }

    @Test
    void getGroups_returnsThemInRepositoryOrder() {
        when(this.watchGroupRepository.findByUserIdOrderByName(USER_ID))
                .thenReturn(List.of(this.group(1L, "Europe"), this.group(2L, "Tech")));

        List<WatchGroupDTO> result = this.watchGroupService.getGroups(USER_ID);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("Europe", result.getFirst().getName());
        Assertions.assertEquals(2L, result.getLast().getId());
    }

    @Test
    void createGroup_trimsTheNameAndAttachesTheUser() {
        when(this.watchGroupRepository.findByUserIdAndName(USER_ID, "Tech")).thenReturn(Optional.empty());
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.watchGroupRepository.saveAndFlush(any(WatchGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WatchGroupDTO result = this.watchGroupService.createGroup(USER_ID, new WatchGroupEditDTO("  Tech  "));

        ArgumentCaptor<WatchGroup> captor = ArgumentCaptor.forClass(WatchGroup.class);
        verify(this.watchGroupRepository).saveAndFlush(captor.capture());
        Assertions.assertEquals("Tech", captor.getValue().getName());
        Assertions.assertEquals(this.user, captor.getValue().getUser());
        Assertions.assertEquals("Tech", result.getName());
    }

    @Test
    void createGroup_rejectsADuplicateName() {
        when(this.watchGroupRepository.findByUserIdAndName(USER_ID, "Tech"))
                .thenReturn(Optional.of(this.group(1L, "Tech")));

        WatchGroupEditDTO request = new WatchGroupEditDTO("Tech");

        ValidationException exception = Assertions.assertThrows(ValidationException.class,
                () -> this.watchGroupService.createGroup(USER_ID, request));

        Assertions.assertEquals("A group with this name already exists", exception.getMessage());
        verify(this.watchGroupRepository, never()).saveAndFlush(any(WatchGroup.class));
    }

    @Test
    void createGroup_treatsADifferentCaseAsADifferentGroup() {
        when(this.watchGroupRepository.findByUserIdAndName(USER_ID, "tech")).thenReturn(Optional.empty());
        when(this.userService.getReference(USER_ID)).thenReturn(this.user);
        when(this.watchGroupRepository.saveAndFlush(any(WatchGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WatchGroupDTO result = this.watchGroupService.createGroup(USER_ID, new WatchGroupEditDTO("tech"));

        Assertions.assertEquals("tech", result.getName());
    }

    @Test
    void updateGroup_renamesAnExistingGroup() {
        WatchGroup existing = this.group(7L, "Tech");
        when(this.watchGroupRepository.findByUserIdAndId(USER_ID, 7L)).thenReturn(Optional.of(existing));
        when(this.watchGroupRepository.findByUserIdAndName(USER_ID, "Technology")).thenReturn(Optional.empty());
        when(this.watchGroupRepository.saveAndFlush(existing)).thenReturn(existing);

        WatchGroupDTO result = this.watchGroupService.updateGroup(USER_ID, 7L, new WatchGroupEditDTO("Technology"));

        Assertions.assertEquals("Technology", existing.getName());
        Assertions.assertEquals("Technology", result.getName());
    }

    @Test
    void updateGroup_allowsRenamingAGroupToItsOwnName() {
        WatchGroup existing = this.group(7L, "Tech");
        when(this.watchGroupRepository.findByUserIdAndId(USER_ID, 7L)).thenReturn(Optional.of(existing));
        when(this.watchGroupRepository.findByUserIdAndName(USER_ID, "Tech")).thenReturn(Optional.of(existing));
        when(this.watchGroupRepository.saveAndFlush(existing)).thenReturn(existing);

        WatchGroupDTO result = this.watchGroupService.updateGroup(USER_ID, 7L, new WatchGroupEditDTO("Tech"));

        Assertions.assertEquals("Tech", result.getName());
    }

    @Test
    void updateGroup_rejectsANameAnotherGroupAlreadyHas() {
        when(this.watchGroupRepository.findByUserIdAndId(USER_ID, 7L)).thenReturn(Optional.of(this.group(7L, "Tech")));
        when(this.watchGroupRepository.findByUserIdAndName(USER_ID, "Europe"))
                .thenReturn(Optional.of(this.group(9L, "Europe")));

        WatchGroupEditDTO request = new WatchGroupEditDTO("Europe");

        Assertions.assertThrows(ValidationException.class,
                () -> this.watchGroupService.updateGroup(USER_ID, 7L, request));
        verify(this.watchGroupRepository, never()).saveAndFlush(any(WatchGroup.class));
    }

    @Test
    void getGroup_throwsWhenTheGroupBelongsToSomeoneElse() {
        when(this.watchGroupRepository.findByUserIdAndId(USER_ID, 99L)).thenReturn(Optional.empty());

        NoSuchElementException exception = Assertions.assertThrows(NoSuchElementException.class,
                () -> this.watchGroupService.getGroup(USER_ID, 99L));

        Assertions.assertEquals("Watch group not found: 99", exception.getMessage());
    }

    @Test
    void deleteGroup_removesTheEntitySoTheCascadeReachesItsWatches() {
        WatchGroup existing = this.group(7L, "Tech");
        when(this.watchGroupRepository.findByUserIdAndId(USER_ID, 7L)).thenReturn(Optional.of(existing));

        Assertions.assertTrue(this.watchGroupService.deleteGroup(USER_ID, 7L));

        verify(this.watchGroupRepository).delete(existing);
    }

    @Test
    void deleteGroup_reportsMissWhenThereIsNothingToDelete() {
        when(this.watchGroupRepository.findByUserIdAndId(USER_ID, 8L)).thenReturn(Optional.empty());

        Assertions.assertFalse(this.watchGroupService.deleteGroup(USER_ID, 8L));

        verify(this.watchGroupRepository, never()).delete(any(WatchGroup.class));
    }
}
