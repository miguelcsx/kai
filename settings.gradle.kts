rootProject.name = "kai"

include(
    "core:kai-domain",
    "core:kai-plugin-api",
    "core:kai-campaign-engine",
    "core:kai-config",
    "storage:kai-storage-api",
    "storage:kai-storage-fs",
    "plugins:strategy:kai-strategy-random",
    "plugins:strategy:kai-strategy-mutation",
    "plugins:executor:kai-executor-cli",
    "plugins:oracle:kai-oracle-crash",
    "plugins:oracle:kai-oracle-differential",
    "plugins:reducer:kai-reducer-delta",
    "plugins:scheduler:kai-scheduler-roundrobin",
    "app:kai-cli",
    "test-support:kai-test-fixtures",
)
