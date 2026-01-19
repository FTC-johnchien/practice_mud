package com.example.htmlmud.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.htmlmud.infra.persistence.entity.ItemTemplateEntity;

public interface ItemTemplateRepository extends JpaRepository<ItemTemplateEntity, Integer> {

}
