package com.squareup.subzero.shared;

import com.squareup.subzero.proto.service.Common.EncryptedPubKey;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;
import java.util.Map;

/**
 * ColdWalletCreator handles the creation of a new cold wallet.  There are two
 * offline steps to this process (InitWallet and FinalizeWallet), which are
 * handled by three static functions here.
 */
public class ColdWalletCreator {

  /** Everything in this class is static, so don't allow construction. */
  private ColdWalletCreator() {}

  /**
   * Create a new wallet.  Call this multiple times, if you want to use multiple tokens.
   *
   * @param token A token for tracking this request.
   * @param walletId An arbitrary wallet ID number, used to identify the wallet in all requests to
   * subzero. A Subzero instance will reject this request if a wallet with that ID already exists.
   * @return The request to send to Subzero.
   */
  public static CommandRequest init(String token, int walletId) {
    return CommandRequest.newBuilder()
        .setToken(token)
        .setWalletId(walletId)
        .setInitWallet(CommandRequest.InitWalletRequest.newBuilder())
        .build();
  }

  /**
   * Combine all of the responses from init.  The token from the response will be copied to the
   * returned CommandRequests so you can match them up.
   *
   * @param tokenToEncryptedPubKeyMap map of Persephone element tokens to those element's encrypted public key.
   * @param elementToken token of element who's request we want to construct.
   * @param walletId id of cold wallet in Subzero that will be finalized with this request.
   * @return FinalizeWallet CommandRequests for Subzero to execute.
   */
  public static CommandRequest combine(
      Map<String, EncryptedPubKey> tokenToEncryptedPubKeyMap, String elementToken, int walletId) {
    if (!tokenToEncryptedPubKeyMap.containsKey(elementToken)) {
      throw new IllegalArgumentException("Map did not contain elementToken: " + elementToken);
    }

    if (tokenToEncryptedPubKeyMap.containsValue(null)) {
      throw new IllegalArgumentException("Map contained a null value");
    }

    if (tokenToEncryptedPubKeyMap.size() != Constants.ENCRYPTED_PUB_KEYS_MAX_COUNT) {
      throw new IllegalArgumentException(String.format("Map should contain %s values, but "
          + "contained %s.", Constants.ENCRYPTED_PUB_KEYS_MAX_COUNT,
          tokenToEncryptedPubKeyMap.size()));
    }

    return CommandRequest.newBuilder()
        .setToken(elementToken)
        .setWalletId(walletId)
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(tokenToEncryptedPubKeyMap.values()))
        .build();
  }

  /**
   * Get the "xpub..." public key, which you can pass to the ColdWallet constructor.
   *
   * @param finalizeWalletResponses The response from Subzero, completing wallet setup.
   * @return A public key string suitable for passing to DeterministicKey.deserializeB58().
   */
  public static String finalize(CommandResponse finalizeWalletResponses) {
    // TODO: In the future, we should have a way of validating some kind of signature
    // so we know these genuinely came from our HSM-backed storage.

    return finalizeWalletResponses.getFinalizeWallet().getPubKey().toStringUtf8();
  }
}
