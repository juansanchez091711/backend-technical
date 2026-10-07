package co.quanta.mrp.bom.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BomItemR2dbcRepository extends ReactiveCrudRepository<BomItemEntity, Long> {

    Flux<BomItemEntity> findByProductIdOrderByIdAsc(Long productId);

    Mono<Boolean> existsByProductIdAndMaterialIgnoreCase(Long productId, String material);
}
