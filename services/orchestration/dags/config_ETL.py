import pendulum
from dataclasses import dataclass
from pathlib import Path
from pyhocon import ConfigFactory

CONFIG_DIR_PATH  = "/opt/airflow/conf"
DAGS_CONFIG_PATH = f"{CONFIG_DIR_PATH}/dags/common.conf"

_dags_config = ConfigFactory.parse_file(Path(DAGS_CONFIG_PATH))

ETL_MIN_ROWS = _dags_config.get_int('Dags.ETL.minRows')

DEFAULT_ARGS = {
    'start_date': pendulum.datetime(2025, 10, 1, 0, 0, 0, tz="Asia/Krasnoyarsk"),
    'email': [_dags_config.get_string('Airflow.email')],
    'email_on_failure': True,
    'retries': 2,
    'retry_delay': pendulum.duration(minutes=5),
}


@dataclass
class ArgDef:
    name: str
    value: str
    is_static: bool = False


BatchArgs = list[str]
BatchGroup = list[BatchArgs]


def grouped_batches(arg: str, total: int, per_group: int) -> list[BatchGroup]:
    batches = [[f"--{arg}", str(i)] for i in range(total)]

    return [batches[i:i + per_group] for i in range(0, total, per_group)]


class Platform:
    def __init__(self,
                 fileName: str,
                 name: str,
                 args: list[ArgDef] = None,
                 with_update: bool = True,
                 module_path: str = None,
                 batch_groups: list[BatchGroup] = None,
                 inter_batch_wait_secs: int = 0
                 ):

        if args is None:
            args = []

        self.fileName = f"{CONFIG_DIR_PATH}/platforms/{fileName}.conf"
        self.name = name
        self.moduleName = module_path if module_path else f"platforms/{name}"

        self.args = [
            ArgDef("savefolder", "Dags.ETL.fileName"),
            ArgDef("conffile", self.fileName, is_static=True),
        ] + args

        self.parts = ["update"] if with_update else []
        self.parts.extend(["extract", "transform-load"])

        self.batch_groups = batch_groups
        self.inter_batch_wait_secs = inter_batch_wait_secs


PLATFORMS = [
    Platform("fn", "Finder"),
    Platform("gm", "GetMatch"),
    Platform("gj", "GeekJob"),
    Platform("hc", "HabrCareer"),
    Platform("hh", "HeadHunter", args=[ArgDef("datefrom", "Dags.ETL.dateFrom")]),
    Platform("az", "Adzuna",
             batch_groups=grouped_batches("locidx", total=6, per_group=3),
             inter_batch_wait_secs=60),
]