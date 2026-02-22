from sqlalchemy import Column, Integer, String, Date
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class Song(Base):

    __tablename__ = "songs"

    id =  Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String, nullable=False)
    artist = Column(String, nullable=False)
    album = Column(String)
    genre = Column(String)
    release_date= Column(Date, nullable=True)
    artwork = Column(String)