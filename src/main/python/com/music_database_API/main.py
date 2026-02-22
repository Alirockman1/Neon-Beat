from fastapi import Depends, FastAPI, HTTPException
from database import Session as SessionLocal
from sqlalchemy.orm import Session
from song_schema import Song
from datetime import date
import database_schema

app = FastAPI()

# Connect to the database
def get_db():
    db = SessionLocal()
    
    try:
        yield db
    finally:
        db.close()

# GET: ALl songs
@app.get("/songs")
def get_all_songs(db: Session = Depends(get_db)):
    db_songs = db.query(database_schema.Song).all()
    return db_songs

# GET: First instance of songs that match by Title and Artist
@app.get("/songs/{title}/{artist}")
def get_song_by_title_artist(title:str, artist:str, db: Session = Depends(get_db)):

    db_song = db.query(database_schema.Song).filter(database_schema.Song.title.ilike(f"%{title}%"), database_schema.Song.artist == artist).first()
    
    if not db_song:
        raise HTTPException(status_code=404, detail="Song not found")
    
    return db_song

# GET: First instance of songs that match by Title
@app.get("/songs/{title}")
def get_song_by_title(title:str, db: Session = Depends(get_db)):

    song_query = db.query(database_schema.Song).filter(database_schema.Song.title.ilike(f"%{title}%"))
    song_count = song_query.count()

    if song_count == 1:
        return song_query.first()
    elif song_count > 1:
        raise HTTPException(status_code=400, detail="Multiple songs found")
    else:
        raise HTTPException(status_code=404, detail="Song not found")

# POST: Add song to database by Title and Artist
@app.post("/songs")
def add_song_by_title_artist(song:Song, db: Session = Depends(get_db)):

    print(song)
    
    title = song.title
    artist = song.artist

    db_song = db.query(database_schema.Song).filter(database_schema.Song.title.ilike(f"%{title}%"), database_schema.Song.artist == artist).first()

    if not db_song:
        new_song = database_schema.Song(**song.model_dump())
        db.add(new_song)
        db.commit()
        db.refresh(new_song)
        return new_song
    
    raise HTTPException(status_code=400, detail="Song already exists")

# PUT: Update song by Title
@app.put("/songs")
def update_song_by_title(song:Song, db: Session = Depends(get_db)):

    title = song.title

    song_query = db.query(database_schema.Song).filter(database_schema.Song.title.ilike(title))
    song_count = song_query.count()

    if song_count > 1:
        raise HTTPException(status_code=400, detail="Multiple songs found")
    
    db_song = song_query.first()
    if not db_song:
        raise HTTPException(status_code=404, detail="Song not found")

    db_song.artist = song.artist
    db_song.album = song.album
    db_song.genre = song.genre
    db_song.release_date = song.release_date
    db_song.artwork = song.artwork

    db.commit()
    db.refresh(db_song)
    return db_song

# PUT: Update song by Title and Artist
@app.put("/songs")
def update_song_by_title_artist(song:Song, db: Session = Depends(get_db)):

    title = song.title
    artist = song.artist

    db_song = db.query(database_schema.Song).filter(database_schema.Song.title.ilike(title), database_schema.Song.artist == artist).first()

    if db_song:
        db_song.album = song.album
        db_song.genre = song.genre
        db_song.release_date = song.release_date
        db_song.artwork = song.artwork
        db.commit()
        db.refresh(db_song)
        return db_song
    else:
        return add_song_by_title_artist(song, title, artist, db)

# DELETE: Delete song by Title and Artist
@app.delete("/songs")
def delete_song_by_title_artist(title:str, artist:str, db: Session = Depends(get_db)):
    
    db_song = db.query(database_schema.Song).filter(database_schema.Song.title.ilike(title), database_schema.Song.artist == artist).first()
    
    if db_song:
        db.delete(db_song)
        db.commit()
        return {"message": "Deleted"}
    
    raise HTTPException(status_code=404, detail="Song not found")   

    













