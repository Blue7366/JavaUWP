#include "forge.h"

#include "loader_common.h"

#include "launcher_common.h"

void ForgeFinalizeVersionInfo(
    MinecraftVersionInfo& info,
    const LaunchTarget& target,
    const LaunchTarget& defaultTarget) {
    (void)target;
    (void)defaultTarget;
    info.supported = false;
    WriteLogF(L"Legacy Forge target %s is not implemented yet", info.targetId.c_str());
}

void ForgeAddJvmOptions(const LoaderJvmContext& ctx, std::vector<std::string>& vmOptions) {
    (void)ctx;
    vmOptions.push_back("-Dforge.logging.console.level=debug");
    vmOptions.push_back("-Dforge.logging.markers=REGISTRIES");
}
