package ke.co.masajr.transport.service;

import ke.co.masajr.transport.entity.Stage;
import ke.co.masajr.transport.repository.StageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StageService {

    private static final Logger log = LoggerFactory.getLogger(StageService.class);

    private final StageRepository stageRepository;

    public StageService(StageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    @Transactional
    public Stage createStage(Long tenantId, String name, String location) {
        log.info("Creating stage '{}' tenantId={}", name, tenantId);
        Stage stage = new Stage();
        stage.setTenantId(tenantId);
        stage.setName(name);
        stage.setLocation(location);
        Stage saved = stageRepository.save(stage);
        log.info("Stage created id={} name='{}'", saved.getId(), saved.getName());
        return saved;
    }

    public List<Stage> listStages(Long tenantId) {
        log.debug("Listing stages tenantId={}", tenantId);
        return stageRepository.findByTenantId(tenantId);
    }
}
