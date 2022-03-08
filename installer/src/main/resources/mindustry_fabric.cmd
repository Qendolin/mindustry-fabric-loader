@echo off
java -Dhttps.protocols=TLSv1.2,TLSv1.1,TLSv1 -XX:+ShowCodeDetailsInExceptionMessages ^
-Dfabric.skipMcProvider=true -Dfabric.side={{ENV_SIDE}} @"{{ARG_FILE}}" {{MAIN_CLASS}}
