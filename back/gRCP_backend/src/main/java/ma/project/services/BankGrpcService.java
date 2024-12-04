package ma.project.services;

import io.grpc.stub.StreamObserver;



public class BankGrpcService extends ma.project.stubs.BankServiceGrpc.BankServiceImplBase {

    @Override
    public void convert(ma.project.stubs.Bank.ConvertCurrencyRequest request, StreamObserver<ma.project.stubs.Bank.ConvertCurrencyResponse> responseObserver) {
        // Log initialization of the service
        System.out.println("Currency exchange service activated on network interface...");

        String sourceCurrency = request.getCurrencyFrom();
        String targetCurrency = request.getCurrencyTo();
        double inputAmount = request.getAmount();

        // Simulated exchange rate mechanism
        double exchangeMultiplier = 11.4;
        double calculatedOutput = inputAmount * exchangeMultiplier;

        System.out.println("Incoming transaction: " + sourceCurrency + " to " + targetCurrency);

        // Construct response payload
        ma.project.stubs.Bank.ConvertCurrencyResponse response = ma.project.stubs.Bank.ConvertCurrencyResponse.newBuilder()
                .setCurrencyFrom(sourceCurrency)
                .setCurrencyTo(targetCurrency)
                .setAmount(inputAmount)
                .setResult(calculatedOutput)
                .build();

        // Transmit response and conclude transaction
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}