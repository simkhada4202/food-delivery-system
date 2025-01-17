package cm.ex.delivery.service;

import cm.ex.delivery.request.StripeRequestDto;
import cm.ex.delivery.response.StripeResponseDto;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.publishable.key}")
    private String publishableKey;

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Autowired
    private BasketServiceImpl basketService;

    public StripeResponseDto checkoutProducts(StripeRequestDto stripeRequestDto) {
        // Set your secret key. Remember to switch to your live secret key in production!
        Stripe.apiKey = secretKey;

        // Create a PaymentIntent with the order amount and currency
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(stripeRequestDto.getName())
                        .build();

        // Create new line item with the above product data and associated price
        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(stripeRequestDto.getCurrency() != null ? stripeRequestDto.getCurrency() : "USD")
                        .setUnitAmount(stripeRequestDto.getAmount())
                        .setProductData(productData)
                        .build();

        // Create new line item with the above price data
        SessionCreateParams.LineItem lineItem =
                SessionCreateParams
                        .LineItem.builder()
                        .setQuantity(stripeRequestDto.getQuantity())
                        .setPriceData(priceData)
                        .build();

        // Create new session with the line items
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl("http://localhost:5173/payment/success")
                        .setCancelUrl("http://localhost:5173/payment/unsuccess")
                        .addLineItem(lineItem)
                        .build();

        // Create new session
        Session session = null;
        try {
            session = Session.create(params);
            basketService.emptyUserBasket();
        } catch (StripeException e) {
            //log the error
        }

        return StripeResponseDto
                .builder()
                .status("SUCCESS")
                .message("Payment session created ")
                .sessionId(session.getId())
                .sessionUrl(session.getUrl())
                .build();
    }

}
