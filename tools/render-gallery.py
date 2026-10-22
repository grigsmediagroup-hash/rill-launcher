"""Arrange actual emulator captures for visual review; never fabricates app screens."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]/'preview'
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',21)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',15)
def montage(filename,items):
    width=300; gutter=24; top=88
    canvas=Image.new('RGB',((width+gutter)*len(items)+gutter,top+560+70),(14,23,30))
    draw=ImageDraw.Draw(canvas)
    draw.text((gutter,20),'RILL  /  ACTUAL ANDROID PREVIEW',font=font,fill=(239,245,244))
    draw.text((gutter,52),'Native views. Android 5+ target. Physical-phone testing pending.',font=small,fill=(163,181,188))
    for index,(source,label) in enumerate(items):
        img=Image.open(ROOT/source).convert('RGB');img.thumbnail((width,560),Image.Resampling.LANCZOS)
        x=gutter+index*(width+gutter);canvas.paste(img,(x,top))
        draw.text((x,top+560+14),label,font=font,fill=(167,227,203))
    canvas.save(ROOT/filename)
montage('themes.png',[('azure.png','Azure'),('slate.png','Slate'),('plum.png','Plum')])
montage('interactions.png',[('dual-clock.png','Clock + battery'),('search.png','Search'),('popup.png','App pop-up'),('widget-home.png','Hosted widget')])
