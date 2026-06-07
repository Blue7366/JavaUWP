#include "minecraft_auth.h"

#include "http_client.h"
#include "launcher_common.h"

#include <winrt/base.h>
#include <winrt/Windows.Security.Credentials.h>

static constexpr char kMicrosoftAuthClientId[] = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb";
static constexpr char kMicrosoftAuthScopes[] = "XboxLive.signin offline_access";
static constexpr wchar_t kRefreshTokenResource[] = L"MinecraftJavaUWP.MicrosoftRefreshToken";
static constexpr wchar_t kRefreshTokenUser[] = L"default";

bool SaveRefreshToken(const std::string& refreshToken) {
    if (refreshToken.empty()) return false;
    try {
        winrt::Windows::Security::Credentials::PasswordVault vault;
        try {
            auto existing = vault.Retrieve(kRefreshTokenResource, kRefreshTokenUser);
            vault.Remove(existing);
        } catch (...) {
        }
        vault.Add(winrt::Windows::Security::Credentials::PasswordCredential(
            kRefreshTokenResource,
            kRefreshTokenUser,
            winrt::to_hstring(refreshToken)));
        WriteLog(L"Saved Microsoft refresh token to Credential Locker");
        return true;
    } catch (const winrt::hresult_error& ex) {
        WriteLogF(L"Failed to save refresh token hr=0x%08X msg=%s",
            static_cast<unsigned int>(ex.code()), ex.message().c_str());
        return false;
    }
}

std::string LoadRefreshToken() {
    try {
        winrt::Windows::Security::Credentials::PasswordVault vault;
        auto credential = vault.Retrieve(kRefreshTokenResource, kRefreshTokenUser);
        credential.RetrievePassword();
        return winrt::to_string(credential.Password());
    } catch (...) {
        return {};
    }
}

void ClearRefreshToken() {
    try {
        winrt::Windows::Security::Credentials::PasswordVault vault;
        auto credential = vault.Retrieve(kRefreshTokenResource, kRefreshTokenUser);
        vault.Remove(credential);
    } catch (...) {
    }
}

bool RequestDeviceCode(DeviceCodeResponse& out, std::string& error) {
    const std::string body = MakeFormBody({
        { "client_id", kMicrosoftAuthClientId },
        { "scope", kMicrosoftAuthScopes }
    });
    const HttpResult response = HttpPostString(
        L"https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode",
        body,
        L"application/x-www-form-urlencoded");
    if (!response.success()) {
        error = "Device code request failed: HTTP " + std::to_string(response.status) + " " + response.body;
        return false;
    }

    out.userCode = ExtractJsonStringValue(response.body, "user_code");
    out.deviceCode = ExtractJsonStringValue(response.body, "device_code");
    out.verificationUri = ExtractJsonStringValue(response.body, "verification_uri");
    out.expiresIn = ExtractJsonIntValue(response.body, "expires_in", 900);
    out.interval = (std::max)(1, ExtractJsonIntValue(response.body, "interval", 5));

    if (out.userCode.empty() || out.deviceCode.empty() || out.verificationUri.empty()) {
        error = "Device code response was missing required fields.";
        return false;
    }

    WriteLogF(L"Device auth code received user_code=%s expires=%d interval=%d",
        a2w(out.userCode.c_str()).c_str(), out.expiresIn, out.interval);
    return true;
}

DevicePollResult PollDeviceToken(const std::string& deviceCode) {
    DevicePollResult result;
    const std::string body = MakeFormBody({
        { "grant_type", "urn:ietf:params:oauth:grant-type:device_code" },
        { "client_id", kMicrosoftAuthClientId },
        { "device_code", deviceCode }
    });
    const HttpResult response = HttpPostString(
        L"https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
        body,
        L"application/x-www-form-urlencoded");

    if (response.success()) {
        result.status = DevicePollStatus::Success;
        result.token.accessToken = ExtractJsonStringValue(response.body, "access_token");
        result.token.refreshToken = ExtractJsonStringValue(response.body, "refresh_token");
        result.token.expiresIn = ExtractJsonIntValue(response.body, "expires_in", 0);
        if (result.token.accessToken.empty()) {
            result.status = DevicePollStatus::Failed;
            result.error = "Microsoft token response did not include access_token.";
        }
        return result;
    }

    const std::string code = ExtractJsonStringValue(response.body, "error");
    if (code == "authorization_pending") {
        result.status = DevicePollStatus::Pending;
    } else if (code == "slow_down") {
        result.status = DevicePollStatus::SlowDown;
    } else {
        result.status = DevicePollStatus::Failed;
        result.error = code.empty()
            ? "Microsoft token polling failed: HTTP " + std::to_string(response.status)
            : code + ": " + ExtractJsonStringValue(response.body, "error_description");
    }
    return result;
}

bool RefreshMicrosoftToken(const std::string& refreshToken, MicrosoftTokenResponse& out, std::string& error) {
    const std::string body = MakeFormBody({
        { "grant_type", "refresh_token" },
        { "client_id", kMicrosoftAuthClientId },
        { "refresh_token", refreshToken },
        { "scope", kMicrosoftAuthScopes }
    });
    const HttpResult response = HttpPostString(
        L"https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
        body,
        L"application/x-www-form-urlencoded");
    if (!response.success()) {
        error = "Saved Microsoft session expired.";
        return false;
    }

    out.accessToken = ExtractJsonStringValue(response.body, "access_token");
    out.refreshToken = ExtractJsonStringValue(response.body, "refresh_token");
    out.expiresIn = ExtractJsonIntValue(response.body, "expires_in", 0);
    if (out.accessToken.empty()) {
        error = "Microsoft refresh response did not include access_token.";
        return false;
    }
    return true;
}

bool AuthenticateWithXboxLive(const std::string& microsoftAccessToken, XboxAuthResponse& out, std::string& error) {
    const std::string payload =
        "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" +
        JsonEscape(microsoftAccessToken) +
        "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
    const HttpResult response = HttpPostString(
        L"https://user.auth.xboxlive.com/user/authenticate",
        payload,
        L"application/json");
    if (!response.success()) {
        error = "Xbox Live auth failed: HTTP " + std::to_string(response.status) + " " + response.body;
        return false;
    }

    out.token = ExtractJsonStringValue(response.body, "Token");
    out.userHash = ExtractJsonStringValue(response.body, "uhs");
    if (out.token.empty() || out.userHash.empty()) {
        error = "Xbox Live auth response was missing token fields.";
        return false;
    }
    return true;
}

bool AuthorizeWithXsts(const std::string& xboxToken, const char* relyingParty, XboxAuthResponse& out, std::string& error) {
    const std::string payload =
        "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" +
        JsonEscape(xboxToken) +
        "\"]},\"RelyingParty\":\"" +
        JsonEscape(relyingParty) +
        "\",\"TokenType\":\"JWT\"}";
    const HttpResult response = HttpPostString(
        L"https://xsts.auth.xboxlive.com/xsts/authorize",
        payload,
        L"application/json");
    if (!response.success()) {
        error = "XSTS auth failed: HTTP " + std::to_string(response.status) + " " + response.body;
        return false;
    }

    out.token = ExtractJsonStringValue(response.body, "Token");
    out.userHash = ExtractJsonStringValue(response.body, "uhs");
    if (out.token.empty() || out.userHash.empty()) {
        error = "XSTS response was missing token fields.";
        return false;
    }
    return true;
}

bool LoginToMinecraft(const std::string& userHash, const std::string& xstsToken, MicrosoftTokenResponse& out, std::string& error) {
    const std::string identity = "XBL3.0 x=" + userHash + ";" + xstsToken;
    const std::string payload = "{\"identityToken\":\"" + JsonEscape(identity) + "\"}";
    const HttpResult response = HttpPostString(
        L"https://api.minecraftservices.com/authentication/login_with_xbox",
        payload,
        L"application/json");
    if (!response.success()) {
        error = "Minecraft login failed: HTTP " + std::to_string(response.status) + " " + response.body;
        return false;
    }

    out.accessToken = ExtractJsonStringValue(response.body, "access_token");
    out.expiresIn = ExtractJsonIntValue(response.body, "expires_in", 0);
    if (out.accessToken.empty()) {
        error = "Minecraft login response did not include access_token.";
        return false;
    }
    return true;
}

bool EnsureMinecraftEntitlement(const std::string& minecraftAccessToken, std::string& error) {
    const HttpResult response = HttpGetBearer(
        L"https://api.minecraftservices.com/entitlements/mcstore",
        minecraftAccessToken);
    if (!response.success()) {
        error = "Minecraft entitlement check failed: HTTP " + std::to_string(response.status) + " " + response.body;
        return false;
    }

    if (response.body.find("\"game_minecraft\"") == std::string::npos &&
        response.body.find("\"product_minecraft\"") == std::string::npos) {
        error = "This Microsoft account does not appear to own Minecraft Java Edition.";
        return false;
    }
    return true;
}

bool FetchMinecraftProfile(const std::string& minecraftAccessToken, LaunchAuthConfig& out, std::string& error) {
    const HttpResult response = HttpGetBearer(
        L"https://api.minecraftservices.com/minecraft/profile",
        minecraftAccessToken);
    if (!response.success()) {
        error = "Minecraft profile request failed: HTTP " + std::to_string(response.status) + " " + response.body;
        return false;
    }

    out.uuid = NormalizeMinecraftUuid(ExtractJsonStringValue(response.body, "id"));
    out.username = ExtractJsonStringValue(response.body, "name");
    out.accessToken = minecraftAccessToken;
    if (out.uuid.empty() || out.username.empty()) {
        error = "Minecraft profile response was missing id or name.";
        return false;
    }
    return true;
}

bool BuildMinecraftAuth(const std::string& microsoftAccessToken, LaunchAuthConfig& out, std::string& error) {
    XboxAuthResponse xbl;
    if (!AuthenticateWithXboxLive(microsoftAccessToken, xbl, error)) {
        return false;
    }

    XboxAuthResponse xsts;
    if (!AuthorizeWithXsts(xbl.token, "rp://api.minecraftservices.com/", xsts, error)) {
        return false;
    }

    MicrosoftTokenResponse minecraftToken;
    if (!LoginToMinecraft(xsts.userHash, xsts.token, minecraftToken, error)) {
        return false;
    }

    if (!EnsureMinecraftEntitlement(minecraftToken.accessToken, error)) {
        return false;
    }

    if (!FetchMinecraftProfile(minecraftToken.accessToken, out, error)) {
        return false;
    }

    WriteLogF(L"Minecraft auth resolved username=%s uuid=%s",
        a2w(out.username.c_str()).c_str(),
        a2w(out.uuid.c_str()).c_str());
    return true;
}
