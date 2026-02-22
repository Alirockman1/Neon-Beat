import os
from dotenv import load_dotenv
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import urllib.parse

# Laod the environment variables
load_dotenv()

# Retrive the variables
username = os.getenv("DB_USERNAME")
password = os.getenv("DB_PASSWORD")
host = os.getenv("DB_HOST")
port = os.getenv("DB_PORT")
name = os.getenv("DB_NAME")

# Protect the password
safe_password = urllib.parse.quote_plus(password)

db_url = f"postgresql://{username}:{password}@{host}:{port}/{name}"

engine = create_engine(db_url)

Session = sessionmaker(autoflush = False, bind=engine)
