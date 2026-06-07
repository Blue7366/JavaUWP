#pragma once

#include <string>
#include <vector>

#include "loader.h"

// Legacy Forge launcher support will be implemented here.
void ForgeFinalizeVersionInfo(
    MinecraftVersionInfo& info,
    const LaunchTarget& target,
    const LaunchTarget& defaultTarget);
void ForgeAddJvmOptions(const LoaderJvmContext& ctx, std::vector<std::string>& vmOptions);
