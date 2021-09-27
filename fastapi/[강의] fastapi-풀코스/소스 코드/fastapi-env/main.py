from fastapi import FastAPI
from typing import Optional
from pydantic import BaseModel

app = FastAPI()


class Blog(BaseModel):
    title: str
    body: str
    published: Optional[bool]


@app.get('/')
def index():
    return {'data': 'blog list'}


@app.get('/about')
def index():
    return {'data': 'about page'}

@app.post('/blog')
def create(blog: Blog):
    return {'data': f'Blog is created with title as {blog.title}'}