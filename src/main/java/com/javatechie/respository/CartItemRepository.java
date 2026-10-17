package com.javatechie.respository;

import com.javatechie.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    CartItem findByProductId(String productId);

    @Transactional
    void deleteByProductId(String productId);
}
