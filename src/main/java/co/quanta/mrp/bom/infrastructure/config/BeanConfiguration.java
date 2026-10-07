package co.quanta.mrp.bom.infrastructure.config;

import co.quanta.mrp.bom.application.service.AddBomItemService;
import co.quanta.mrp.bom.application.service.CalculateProductionService;
import co.quanta.mrp.bom.application.service.CreateProductService;
import co.quanta.mrp.bom.application.service.GetOperatorsService;
import co.quanta.mrp.bom.domain.port.in.AddBomItemUseCase;
import co.quanta.mrp.bom.domain.port.in.CalculateProductionUseCase;
import co.quanta.mrp.bom.domain.port.in.CreateProductUseCase;
import co.quanta.mrp.bom.domain.port.in.GetOperatorsUseCase;
import co.quanta.mrp.bom.domain.port.out.BomItemRepositoryPort;
import co.quanta.mrp.bom.domain.port.out.OperatorClientPort;
import co.quanta.mrp.bom.domain.port.out.ProductRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free application services as Spring beans. */
@Configuration
public class BeanConfiguration {

    @Bean
    public CreateProductUseCase createProductUseCase(ProductRepositoryPort productRepository) {
        return new CreateProductService(productRepository);
    }

    @Bean
    public AddBomItemUseCase addBomItemUseCase(ProductRepositoryPort productRepository,
                                               BomItemRepositoryPort bomItemRepository) {
        return new AddBomItemService(productRepository, bomItemRepository);
    }

    @Bean
    public CalculateProductionUseCase calculateProductionUseCase(ProductRepositoryPort productRepository,
                                                                 BomItemRepositoryPort bomItemRepository) {
        return new CalculateProductionService(productRepository, bomItemRepository);
    }

    @Bean
    public GetOperatorsUseCase getOperatorsUseCase(OperatorClientPort operatorClient) {
        return new GetOperatorsService(operatorClient);
    }
}
