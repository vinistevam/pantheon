package tech.pegasys.pantheon.tests.acceptance.dsl.transaction.eea;

import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransaction;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PrivateTransactionFactory {

    private static BytesValue EVENT_EMITTER_CONSTRUCTOR = BytesValue.fromHexString(
            "0x608060405234801561001057600080fd5b5060008054600160a06"
                    + "0020a03191633179055610199806100326000396000f3fe6080"
                    + "604052600436106100565763ffffffff7c01000000000000000"
                    + "000000000000000000000000000000000000000006000350416"
                    + "633fa4f245811461005b5780636057361d1461008257806367e"
                    + "404ce146100ae575b600080fd5b34801561006757600080fd5b"
                    + "506100706100ec565b60408051918252519081900360200190f"
                    + "35b34801561008e57600080fd5b506100ac6004803603602081"
                    + "10156100a557600080fd5b50356100f2565b005b3480156100b"
                    + "a57600080fd5b506100c3610151565b6040805173ffffffffff"
                    + "ffffffffffffffffffffffffffffff909216825251908190036"
                    + "0200190f35b60025490565b6040805133815260208101839052"
                    + "81517fc9db20adedc6cf2b5d25252b101ab03e124902a73fcb1"
                    + "2b753f3d1aaa2d8f9f5929181900390910190a1600255600180"
                    + "5473ffffffffffffffffffffffffffffffffffffffff1916331"
                    + "79055565b60015473ffffffffffffffffffffffffffffffffff"
                    + "ffffff169056fea165627a7a72305820c7f729cb24e05c221f5"
                    + "aa913700793994656f233fe2ce3b9fd9a505ea17e8d8a0029");

    private static BytesValue SET_FUNCTION_CALL = BytesValue.fromHexString(
            "0x6057361d00000000000000000000000000000000000000000000000000000000000003e8");

    private static BytesValue GET_FUNCTION_CALL = BytesValue.fromHexString("0x3fa4f245");

    public PrivateTransaction createContractTransaction(
            final long nonce,
            final Address from,
            final BytesValue privateFrom,
            final List<BytesValue> privateFor,
            final SECP256K1.KeyPair keypair) {
        return privateTransaction(nonce, null, EVENT_EMITTER_CONSTRUCTOR, from, privateFrom, privateFor, keypair);

    }

    public PrivateTransaction storeFunctionTransaction(
            final long nonce,
            final Address to,
            final Address from,
            final BytesValue privateFrom,
            final List<BytesValue> privateFor,
            final SECP256K1.KeyPair keypair) {
        return privateTransaction(nonce, to, SET_FUNCTION_CALL, from, privateFrom, privateFor, keypair);

    }

    public PrivateTransaction getFunctionTransaction(
            final long nonce,
            final Address to,
            final Address from,
            final BytesValue privateFrom,
            final List<BytesValue> privateFor,
            final SECP256K1.KeyPair keypair) {
        return privateTransaction(nonce, to, GET_FUNCTION_CALL, from, privateFrom, privateFor, keypair);
    }

    public PrivateTransaction privateTransaction(
            final long nonce,
            final Address to,
            final BytesValue payload,
            final Address from,
            final BytesValue privateFrom,
            final List<BytesValue> privateFor,
            final SECP256K1.KeyPair keypair) {
        return PrivateTransaction.builder()
                .nonce(nonce)
                .gasPrice(Wei.ZERO)
                .gasLimit(3000000)
                .to(to)
                .value(Wei.ZERO)
                .payload(payload)
                .sender(from)
                .chainId(2018)
                .privateFrom(privateFrom)
                .privateFor(privateFor)
                .restriction(BytesValue.wrap("restricted".getBytes(UTF_8)))
                .signAndBuild(keypair);
    }
}
