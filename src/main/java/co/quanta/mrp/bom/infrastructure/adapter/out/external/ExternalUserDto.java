package co.quanta.mrp.bom.infrastructure.adapter.out.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Subset of a jsonplaceholder {@code /users} entry; nested address/company are ignored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalUserDto(Long id, String name, String email, String phone) {
}
