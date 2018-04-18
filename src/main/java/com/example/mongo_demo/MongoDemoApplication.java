package com.example.mongo_demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class MongoDemoApplication {

    @Bean
    ApplicationRunner demo(OrderRepository orderRepository) {
        return args -> {
            orderRepository.deleteAll();
            Stream.of("customerA", "customerB")
                    .map(Order::new)
                    .map(orderRepository::save)
                    .forEach(System.out::println);
        };
    }
    
	public static void main(String[] args) {
		SpringApplication.run(MongoDemoApplication.class, args);
	}
}

@RestController
class OrderController {
    @Autowired OrderRepository orderRepository;
    @GetMapping("/order")
    public Iterable<Order> findAll() {
        return orderRepository.findAll();
    }
    @GetMapping("/order/{id}")
    public Optional<Order> findById(@PathVariable String id) {
        return orderRepository.findById(id);
    }
    @PostMapping("/order")
    public Order save(@RequestBody Order order) {
        return orderRepository.save(order);
    }
    @PostMapping("/order/{id}/item")
    public void saveItem(@PathVariable String id, @RequestBody Item item) {
//        orderRepository.addItem(id, item);
        orderRepository.findById(id).ifPresent(order -> {
            try {
                Thread.sleep(10_000L);
            } catch (InterruptedException ex) {
                Logger.getLogger(OrderController.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (order.getItems() == null) order.setItems(new ArrayList<>());
            order.getItems().add(item);
            orderRepository.save(order);
        });
    }
}

interface OrderRepository extends CrudRepository<Order, String>, OrderRepositoryCustom {
    Optional<Order> findByCustomerId(String customerId);
}

interface OrderRepositoryCustom {
    void addItem(String id, Item item);
}

class OrderRepositoryCustomImpl implements OrderRepositoryCustom {
    @Autowired MongoOperations mongoOperations;
    @Override public void addItem(String id, Item item) {
        mongoOperations.upsert(query(where("_id").is(id)), new Update().push("items", item), Order.class);
    }
}


@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Document
class Order {
    @Id private String id; 
    @NonNull private String customerId;
    private List<Item> items;
    @Version private int version;
}

@Data
@AllArgsConstructor
class Item {
    private String product;
    private long value;
}