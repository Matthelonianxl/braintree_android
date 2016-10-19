package com.braintreepayments.api;

import android.content.Intent;

import com.braintreepayments.api.exceptions.ConfigurationException;
import com.braintreepayments.api.interfaces.ConfigurationListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCallback;
import com.braintreepayments.api.models.BraintreeRequestCodes;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.VisaCheckoutConfiguration;
import com.braintreepayments.api.models.VisaCheckoutPaymentBuilder;
import com.visa.checkout.VisaLibrary;
import com.visa.checkout.VisaMcomLibrary;
import com.visa.checkout.VisaMerchantInfo;
import com.visa.checkout.VisaMerchantInfo.MerchantDataLevel;
import com.visa.checkout.VisaPaymentInfo;
import com.visa.checkout.VisaPaymentSummary;
import com.visa.checkout.utils.VisaEnvironmentConfig;

public class VisaCheckout {
    public static void createVisaCheckoutLibrary(final BraintreeFragment braintreeFragment) {
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                VisaCheckoutConfiguration visaCheckoutConfiguration = configuration.getVisaCheckout();

                if (!visaCheckoutConfiguration.isEnabled()) {
                    braintreeFragment.postCallback(new ConfigurationException("Visa Checkout is not enabled."));
                    return;
                }

                VisaEnvironmentConfig visaEnvironmentConfig = VisaEnvironmentConfig.SANDBOX;

                if ("production".equals(configuration.getEnvironment())) {
                    visaEnvironmentConfig = VisaEnvironmentConfig.PRODUCTION;
                }

                visaEnvironmentConfig.setMerchantApiKey(configuration.getVisaCheckout().getApiKey());
                visaEnvironmentConfig.setVisaCheckoutRequestCode(BraintreeRequestCodes.VISA_CHECKOUT);

                VisaMcomLibrary visaMcomLibrary = VisaMcomLibrary.getLibrary(braintreeFragment.getActivity(),
                        visaEnvironmentConfig);

                braintreeFragment.postVisaCheckoutLibraryCallback(visaMcomLibrary);
            }
        });

    }

    public static void authorize(final BraintreeFragment braintreeFragment, final VisaMcomLibrary visaMcomLibrary, final VisaPaymentInfo visaPaymentInfo) {
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                VisaMerchantInfo visaMerchantInfo = visaPaymentInfo.getVisaMerchantInfo();
                if (visaMerchantInfo == null) {
                    visaMerchantInfo = new VisaMerchantInfo();
                }

                visaMerchantInfo.setMerchantApiKey(configuration.getVisaCheckout().getApiKey());
                visaMerchantInfo.setDataLevel(MerchantDataLevel.FULL);
                visaPaymentInfo.setVisaMerchantInfo(visaMerchantInfo);

                visaPaymentInfo.setExternalClientId(configuration.getVisaCheckout().getExternalClientId());

                visaMcomLibrary.checkoutWithPayment(visaPaymentInfo, BraintreeRequestCodes.VISA_CHECKOUT);
            }
        });
    }

    protected static void onActivityResult(BraintreeFragment braintreeFragment, int resultCode, Intent data) {
        // Process data
        VisaPaymentSummary visaPaymentSummary = data.getParcelableExtra(VisaLibrary.PAYMENT_SUMMARY);
        tokenize(braintreeFragment, visaPaymentSummary);
    }

    public static void tokenize(final BraintreeFragment braintreeFragment, final VisaPaymentSummary visaPaymentSummary) {
        braintreeFragment.waitForConfiguration(new ConfigurationListener() {
            @Override
            public void onConfigurationFetched(Configuration configuration) {
                TokenizationClient.tokenize(braintreeFragment, new VisaCheckoutPaymentBuilder(visaPaymentSummary),
                        new PaymentMethodNonceCallback() {
                            @Override
                            public void success(PaymentMethodNonce paymentMethodNonce) {
                                // TODO analytics
                                braintreeFragment.postCallback(paymentMethodNonce);
                            }

                            @Override
                            public void failure(Exception exception) {
                                // TODO analytics
                                braintreeFragment.postCallback(exception);
                            }
                        });
            }
        });
    }

}
