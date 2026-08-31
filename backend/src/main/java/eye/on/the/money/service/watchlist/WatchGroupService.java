package eye.on.the.money.service.watchlist;

import eye.on.the.money.dto.in.WatchGroupEditDTO;
import eye.on.the.money.dto.out.WatchGroupDTO;
import eye.on.the.money.exception.ValidationException;
import eye.on.the.money.model.watchlist.WatchGroup;
import eye.on.the.money.repository.watchlist.WatchGroupRepository;
import eye.on.the.money.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WatchGroupService {

    private static final String DUPLICATE_MESSAGE = "A group with this name already exists";

    private final WatchGroupRepository watchGroupRepository;
    private final UserService userService;

    public List<WatchGroupDTO> getGroups(Long userId) {
        log.trace("Enter");
        return this.watchGroupRepository.findByUserIdOrderByName(userId).stream()
                .map(this::convertToDTO).toList();
    }

    public WatchGroup getGroup(Long userId, Long id) {
        log.trace("Enter");
        return this.watchGroupRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> new NoSuchElementException("Watch group not found: " + id));
    }

    @Transactional
    public WatchGroupDTO createGroup(Long userId, WatchGroupEditDTO editDTO) {
        log.trace("Enter");
        String name = editDTO.name().trim();
        this.rejectDuplicate(userId, name, null);

        WatchGroup group = WatchGroup.builder()
                .name(name)
                .user(this.userService.getReference(userId))
                .build();

        return this.convertToDTO(this.save(group));
    }

    @Transactional
    public WatchGroupDTO updateGroup(Long userId, Long id, WatchGroupEditDTO editDTO) {
        log.trace("Enter");
        String name = editDTO.name().trim();
        WatchGroup group = this.getGroup(userId, id);
        this.rejectDuplicate(userId, name, id);

        group.setName(name);
        return this.convertToDTO(this.save(group));
    }

    @Transactional
    public boolean deleteGroup(Long userId, Long id) {
        log.trace("Enter");
        Optional<WatchGroup> group = this.watchGroupRepository.findByUserIdAndId(userId, id);
        group.ifPresent(this.watchGroupRepository::delete);
        return group.isPresent();
    }

    private WatchGroup save(WatchGroup group) {
        try {
            return this.watchGroupRepository.saveAndFlush(group);
        } catch (DataIntegrityViolationException e) {
            log.info("Rejected duplicate watch group {}", group.getName());
            throw new ValidationException(DUPLICATE_MESSAGE);
        }
    }

    private void rejectDuplicate(Long userId, String name, Long selfId) {
        this.watchGroupRepository.findByUserIdAndName(userId, name)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new ValidationException(DUPLICATE_MESSAGE);
                });
    }

    private WatchGroupDTO convertToDTO(WatchGroup group) {
        return WatchGroupDTO.builder().id(group.getId()).name(group.getName()).build();
    }
}
