from typing import Optional
from pydantic import BaseModel, Field, ConfigDict, field_validator
from datetime import date

class Song(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra='ignore')

    # id: int
    title: str
    artist: str
    album: Optional[str] = "Unknown"
    genre: Optional[str] = "Unknown"
    release_date: Optional[date] = Field(None, alias="date of release")
    artwork: str

    @field_validator('release_date', mode='before')
    @classmethod
    def parse_itunes_date(cls, v):
            if v == "N/A" or v == "" or v is None:
                return None
            if isinstance(v, str):
                # Handle ISO timestamp: 2026-02-10T12:00:00
                if 'T' in v:
                    return v.split('T')[0]
                # Handle just the year: 2026
                if len(v) == 4 and v.isdigit():
                    return f"{v}-01-01"
            return v