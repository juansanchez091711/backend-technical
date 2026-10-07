package co.quanta.mrp.bom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

@SpringBootTest
class BomApplicationTests {

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void contextLoadsAndSchemaIsCreated() {
        var tables = databaseClient.sql("""
                        SELECT COUNT(*) AS total FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_NAME IN ('PRODUCT', 'BOM_ITEM')
                        """)
                .map(row -> row.get("total", Long.class))
                .one();

        StepVerifier.create(tables)
                .expectNext(2L)
                .verifyComplete();
    }
}
