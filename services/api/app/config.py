from pydantic_settings import BaseSettings


class Settings(BaseSettings):

    ch_host: str
    ch_port: int = 8123
    ch_user: str
    ch_pass: str
    ch_db: str

    api_host: str = "0.0.0.0"
    api_port: int = 8000

    cors_origins: str = "*"

    @property
    def cors_origin_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    model_config = {"env_prefix": "", "case_sensitive": False}


settings = Settings()
