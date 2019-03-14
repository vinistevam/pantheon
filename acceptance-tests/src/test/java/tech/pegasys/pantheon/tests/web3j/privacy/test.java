/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.tests.web3j.privacy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransaction;
import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.web3j.crypto.Credentials;

public class test {

  protected static final Address CONTRACT_ADDRESS =
      Address.fromHexString("0x0bac79b78b9866ef11c989ad21a7fcf15f7a18d7");

  protected static final String PUBLIC_KEY_1 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";
  protected static final String PUBLIC_KEY_2 = "Ko2bVqD+nNlNYL5EE7y3IdOnviftjiizpjRt+HTuFBs=";
  protected static final String PUBLIC_KEY_3 = "A1aVtMxLCUHmBVHXoZzzBgPbW/wj5axDpW9X8l91SGo=";

  @Test
  public void test() {
    SECP256K1.KeyPair keyPair =
        SECP256K1.KeyPair.create(
            SECP256K1.PrivateKey.create(
                new BigInteger(
                    "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63", 16)));
    SECP256K1.KeyPair keyPair1 =
        SECP256K1.KeyPair.create(
            SECP256K1.PrivateKey.create(
                new BigInteger(
                    "c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3", 16)));
    SECP256K1.KeyPair keyPair2 =
        SECP256K1.KeyPair.create(
            SECP256K1.PrivateKey.create(
                new BigInteger(
                    "ae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f", 16)));
    Address address =
        Address.fromHexString(
            Credentials.create("8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63")
                .getAddress());
    Address address1 =
        Address.fromHexString(
            Credentials.create("c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3")
                .getAddress());
    Address address2 =
        Address.fromHexString(
            Credentials.create("ae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f")
                .getAddress());

    final PrivateTransaction DEPLOY_CONTRACT =
        PrivateTransaction.builder()
            .nonce(0)
            .gasPrice(Wei.of(0))
            .gasLimit(3000000)
            .to(null)
            .value(Wei.ZERO)
            .payload(
                BytesValue.fromHexString(
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
                        + "aa913700793994656f233fe2ce3b9fd9a505ea17e8d8a0029"))
            .sender(address)
            .chainId(2018)
            .privateFrom(BytesValue.wrap(PUBLIC_KEY_1.getBytes(UTF_8)))
            .privateFor(Lists.newArrayList())
            .restriction(BytesValue.wrap("restricted".getBytes(UTF_8)))
            .signAndBuild(
                SECP256K1.KeyPair.create(
                    SECP256K1.PrivateKey.create(
                        new BigInteger(
                            "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
                            16))));

    final PrivateTransaction SET_FUNCTION_CALL =
        PrivateTransaction.builder()
            .nonce(1)
            .gasPrice(Wei.of(0))
            .gasLimit(3000000)
            .to(CONTRACT_ADDRESS)
            .value(Wei.ZERO)
            .payload(
                BytesValue.fromHexString(
                    "0x6057361d00000000000000000000000000000000000000000000000000000000000003e8"))
            .sender(address1)
            .chainId(2018)
            .privateFrom(BytesValue.wrap(PUBLIC_KEY_1.getBytes(UTF_8)))
            .privateFor(Lists.newArrayList())
            .restriction(BytesValue.wrap("restricted".getBytes(UTF_8)))
            .signAndBuild(
                SECP256K1.KeyPair.create(
                    SECP256K1.PrivateKey.create(
                        new BigInteger(
                            "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
                            16))));

    final PrivateTransaction GET_FUNCTION_CALL =
        PrivateTransaction.builder()
            .nonce(2)
            .gasPrice(Wei.of(0))
            .gasLimit(3000000)
            .to(CONTRACT_ADDRESS)
            .value(Wei.ZERO)
            .payload(BytesValue.fromHexString("0x3fa4f245"))
            .sender(address)
            .chainId(2018)
            .privateFrom(BytesValue.wrap(PUBLIC_KEY_1.getBytes(UTF_8)))
            .privateFor(Lists.newArrayList())
            .restriction(BytesValue.wrap("restricted".getBytes(UTF_8)))
            .signAndBuild(
                SECP256K1.KeyPair.create(
                    SECP256K1.PrivateKey.create(
                        new BigInteger(
                            "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
                            16))));

    assertEquals("", "");
  }

  @Test
  public void test1() {
    SECP256K1.KeyPair keyPair =
        SECP256K1.KeyPair.create(
            SECP256K1.PrivateKey.create(
                new BigInteger(
                    "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63", 16)));
    SECP256K1.KeyPair keyPair1 =
        SECP256K1.KeyPair.create(
            SECP256K1.PrivateKey.create(
                new BigInteger(
                    "c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3", 16)));
    SECP256K1.KeyPair keyPair2 =
        SECP256K1.KeyPair.create(
            SECP256K1.PrivateKey.create(
                new BigInteger(
                    "ae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f", 16)));
    Address address =
        Address.fromHexString(
            Credentials.create("8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63")
                .getAddress());
    Address address1 =
        Address.fromHexString(
            Credentials.create("c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3")
                .getAddress());
    Address address2 =
        Address.fromHexString(
            Credentials.create("ae6ae8e5ccbfb04590405997ee2d52d2b330726137b875053c36d94e974d162f")
                .getAddress());

    final PrivateTransaction DEPLOY_CONTRACT =
        PrivateTransaction.builder()
            .nonce(0)
            .gasPrice(Wei.of(0))
            .gasLimit(3000000)
            .to(null)
            .value(Wei.ZERO)
            .payload(
                BytesValue.fromHexString(
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
                        + "aa913700793994656f233fe2ce3b9fd9a505ea17e8d8a0029"))
            .sender(address)
            .chainId(2018)
            .privateFrom(BytesValue.wrap(PUBLIC_KEY_1.getBytes(UTF_8)))
            .privateFor(Lists.newArrayList(BytesValue.wrap(PUBLIC_KEY_2.getBytes(UTF_8))))
            .restriction(BytesValue.wrap("restricted".getBytes(UTF_8)))
            .signAndBuild(
                SECP256K1.KeyPair.create(
                    SECP256K1.PrivateKey.create(
                        new BigInteger(
                            "8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
                            16))));

    final PrivateTransaction SET_FUNCTION_CALL =
        PrivateTransaction.builder()
            .nonce(0)
            .gasPrice(Wei.of(0))
            .gasLimit(3000000)
            .to(CONTRACT_ADDRESS)
            .value(Wei.ZERO)
            .payload(
                BytesValue.fromHexString(
                    "0x6057361d00000000000000000000000000000000000000000000000000000000000003e8"))
            .sender(address1)
            .chainId(2018)
            .privateFrom(BytesValue.wrap(PUBLIC_KEY_2.getBytes(UTF_8)))
            .privateFor(Lists.newArrayList(BytesValue.wrap(PUBLIC_KEY_1.getBytes(UTF_8))))
            .restriction(BytesValue.wrap("restricted".getBytes(UTF_8)))
            .signAndBuild(
                SECP256K1.KeyPair.create(
                    SECP256K1.PrivateKey.create(
                        new BigInteger(
                            "c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3",
                            16))));

    final PrivateTransaction GET_FUNCTION_CALL =
        PrivateTransaction.builder()
            .nonce(1)
            .gasPrice(Wei.of(0))
            .gasLimit(3000000)
            .to(CONTRACT_ADDRESS)
            .value(Wei.ZERO)
            .payload(BytesValue.fromHexString("0x3fa4f245"))
            .sender(address1)
            .chainId(2018)
            .privateFrom(BytesValue.wrap(PUBLIC_KEY_2.getBytes(UTF_8)))
            .privateFor(Lists.newArrayList(BytesValue.wrap(PUBLIC_KEY_1.getBytes(UTF_8))))
            .restriction(BytesValue.wrap("restricted".getBytes(UTF_8)))
            .signAndBuild(
                SECP256K1.KeyPair.create(
                    SECP256K1.PrivateKey.create(
                        new BigInteger(
                            "c87509a1c067bbde78beb793e6fa76530b6382a4c0241e5e4a9ec0a0f44dc0d3",
                            16))));

    assertEquals("", "");
  }

  private String toRlp(final PrivateTransaction transaction) {
    BytesValueRLPOutput bvrlpo = new BytesValueRLPOutput();
    transaction.writeTo(bvrlpo);
    return bvrlpo.encoded().toString();
  }
}
