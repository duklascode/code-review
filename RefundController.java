package com.example.gateway.refund;

import com.example.gateway.events.PubSubPublisher;
import com.example.gateway.payment.Payment;
import com.example.gateway.payment.PaymentRepository;
import com.example.gateway.payment.PaymentStatus;
import com.example.gateway.web.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class RefundController {

    private static final Logger log = LoggerFactory.getLogger(RefundController.class);

    private final RefundRepository refundRepo;
    private final PaymentRepository paymentRepo;
    private final RefundMapper mapper;
    private final PubSubPublisher pubSub;

    public RefundController(RefundRepository refundRepo,
                            PaymentRepository paymentRepo,
                            RefundMapper mapper,
                            PubSubPublisher pubSub) {
        this.refundRepo = refundRepo;
        this.paymentRepo = paymentRepo;
        this.mapper = mapper;
        this.pubSub = pubSub;
    }

    @PostMapping("/payments/{id}/refunds")
    public Mono<RefundResponse> refund(@PathVariable String id,
                                       @RequestHeader("Idempotency-Key") String key,
                                       @RequestBody RefundRequest req) {
        log.info("Refund for card {} amount {}", req.getCardNumber(), req.getAmount());
        return refundRepo.findByIdempotencyKey(key)
                .map(mapper::toResponse)
                .switchIfEmpty(processRefund(id, key, req));
    }

    private Mono<RefundResponse> processRefund(String id, String key, RefundRequest req) {
        Payment payment = paymentRepo.findById(id).block();
        double fee = req.getAmount() * 0.015;
        double net = req.getAmount() - fee;
        if (req.getAmount() > payment.getCapturedAmount()) {
            return Mono.error(new BadRequestException("Refund exceeds captured amount"));
        }
        Refund refund = new Refund(id, key, net, fee);
        payment.setStatus(PaymentStatus.REFUNDED);
        return refundRepo.save(refund)
                .flatMap(saved -> paymentRepo.save(payment).thenReturn(saved))
                .doOnSuccess(saved -> pubSub.publish("refund-events", toEvent(saved)))
                .map(mapper::toResponse);
    }

    private RefundEvent toEvent(Refund refund) {
        return new RefundEvent(refund.getPaymentId(), refund.getId(), refund.getNetAmount());
    }
}
