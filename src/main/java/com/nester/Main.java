// Main.java
package com.nester;

import com.nester.model.User;
import com.nester.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Создаем администратора
            if (userRepository.findByLogin("admin").isEmpty()) {
                User admin = new User();
                admin.setLogin("admin");
                admin.setFullName("Администратор Системы");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("ADMIN");
                admin.setActive(true);
                userRepository.save(admin);
                System.out.println("✅ Создан администратор: admin / admin123");
            }

            // Создаем работника склада
            if (userRepository.findByLogin("worker").isEmpty()) {
                User worker = new User();
                worker.setLogin("worker");
                worker.setFullName("Иванов Иван Иванович");
                worker.setPassword(passwordEncoder.encode("worker123"));
                worker.setRole("WORKER");
                worker.setActive(true);
                worker.setWarehouseId("WAREHOUSE_001");
                userRepository.save(worker);
                System.out.println("✅ Создан работник склада: worker / worker123");
            }

            // Создаем сотрудника производственной линии
            if (userRepository.findByLogin("employee").isEmpty()) {
                User employee = new User();
                employee.setLogin("employee");
                employee.setFullName("Петров Петр Петрович");
                employee.setPassword(passwordEncoder.encode("employee123"));
                employee.setRole("EMPLOYEE");
                employee.setActive(true);
                userRepository.save(employee);
                System.out.println("✅ Создан сотрудник участка: employee / employee123");
            }

            // Создаем менеджера
            if (userRepository.findByLogin("manager").isEmpty()) {
                User manager = new User();
                manager.setLogin("manager");
                manager.setFullName("Сидорова Анна Сергеевна");
                manager.setPassword(passwordEncoder.encode("manager123"));
                manager.setRole("MANAGER");
                manager.setActive(true);
                manager.setManagedWarehouseIds(List.of("WAREHOUSE_001"));
                userRepository.save(manager);
                System.out.println("✅ Создан менеджер: manager / manager123");
            }

            System.out.println("========================================");
            System.out.println("📋 Тестовые учетные записи:");
            System.out.println("   Администратор: admin / admin123");
            System.out.println("   Работник склада: worker / worker123");
            System.out.println("   Сотрудник участка: employee / employee123");
            System.out.println("   Менеджер: manager / manager123");
            System.out.println("========================================");
        };
    }
}