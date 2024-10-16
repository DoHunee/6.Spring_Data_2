package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@Slf4j
@SpringBootTest
public class BasicTxTest {
    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    // 트랜잭션을 시작한 후 커밋하고, 커밋 완료 메시지가 출력되는지 확인.
    @Test
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    // 트랜잭션을 시작한 후 롤백하고, 롤백 완료 메시지가 출력되는지 확인.
    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 롤백 완료");
    }

    // 두 개의 독립적인 트랜잭션이 순차적으로 정상적으로 커밋되는지 확인.
    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);
        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    // 두 개의 트랜잭션을 연속으로 처리하면서, 첫 번째 트랜잭션은 커밋하고, 두 번째 트랜잭션은 롤백할 때 어떻게 동작하는지 확인.
    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);
        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }

    // 외부 트랜잭션과 내부 트랜잭션을 처리하면서, 두 트랜잭션이 독립적으로 커밋될 때 정상적으로 동작하는지 확인하는 테스트.
    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());
        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);
        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }

    // 외부 트랜잭션과 내부 트랜잭션을 연속으로 처리하면서, 내부 트랜잭션은 커밋하고 외부 트랜잭션을 롤백할 때 전체 트랜잭션에 미치는 영향을
    // 확인.
    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);
        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);
    }

    // 내부 트랜잭션에서 롤백이 발생한 후 외부 트랜잭션을 커밋하려고 할 때 발생하는 예외 상황을 확인하는 테스트.
    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);
        log.info("외부 트랜잭션 커밋");
        assertThatThrownBy(() -> txManager.commit(outer)).isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());
        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
 
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
 
        TransactionStatus inner = txManager.getTransaction(definition);
       log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
 
       log.info("내부 트랜잭션 롤백");
       txManager.rollback(inner); //롤백
       
       log.info("외부 트랜잭션 커밋");
       txManager.commit(outer); //커밋
    }

}