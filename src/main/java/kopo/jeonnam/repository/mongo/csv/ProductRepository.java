package kopo.jeonnam.repository.mongo.csv;

import kopo.jeonnam.model.Product;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 📚 Product 도메인을 위한 MongoDB 레포지토리
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, ObjectId> {
    // 기본 CRUD 제공

    List<Product> findByProAreaContainingIgnoreCaseAndProNameContainingIgnoreCase(String area, String name);
}
