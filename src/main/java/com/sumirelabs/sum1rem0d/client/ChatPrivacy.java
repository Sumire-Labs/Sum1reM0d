package com.sumirelabs.sum1rem0d.client;

import com.sumirelabs.sum1rem0d.config.PersonalConfig;

public final class ChatPrivacy {
  private ChatPrivacy() {}

  public static boolean suppressSigning() {
    try {
      return PersonalConfig.store().bool("no_chat_reports", "suppress_signing");
    } catch (IllegalStateException notInitialized) {
      return false;
    }
  }
}
