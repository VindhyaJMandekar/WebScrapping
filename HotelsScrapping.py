# -*- coding: utf-8 -*-
#import libraries
import time
from selenium import webdriver
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
import codecs
from bs4 import BeautifulSoup as bs
import requests
import pymongo
import plotly.express as px
from urllib.parse import quote
import requests
from pymongo import MongoClient
from tqdm import tqdm
import pandas as pd
import re
tqdm.pandas()

def get_search_results():
    #Hit the search page, load results and save the html file
    driver = webdriver.Chrome()
    driver.get("https://www.hotels.com/Hotel-Search?adults=2&children=&destination=San%20Francisco%2C%20California%2C%20United%20States%20of%20America&endDate=2023-04-04&latLong=&mapBounds=&pwaDialog&regionId=3132&rfrrid=TG.LP.Hotels.Hotel&semdtl=&sort=RECOMMENDED&startDate=2023-04-03&theme=&useRewards=false&userIntent=")
    for i in range(1,9):
        elements = WebDriverWait(driver, 20).until(EC.visibility_of_all_elements_located((By.XPATH, "//button[@data-stid='show-more-results']")))
        for elem in elements:
            elem.click()
        print(i)
        time.sleep(10)
    h = driver.page_source
    f = open("Hotels.html", "x")
    with open("Hotels.html", "w", encoding="utf-8") as f:
        f.write(h)
    f.close()
    time.sleep(10)
    driver.quit()



#from the saved file, get the urls of the hotels and save the individual pages for each hotel on your local disk

def get_hotel_pages():
    with open("Hotels.html") as fp:
        soup = bs(fp, 'html.parser')
    #print(soup)
    data = soup.findAll('div',attrs={'data-stid':'property-listing-results'})
    hotels=[]
    for div in data:
        links = div.findAll('a',attrs={'data-stid':'open-hotel-information'})
        for a in links:
            hotels.append(a['href'])
    
    more_data = soup.findAll('div',attrs={'class':'lazyload-wrapper', 'data-stid':'open-hotel-information'})
    for div in more_data:
        #links = div.findAll('a',attrs={'data-stid':'open-hotel-information'})
        for a in div:
            hotels.append(a['href'])
    hotels = list(set(hotels))
    #print(len(hotels))
    counter = len(hotels)
    
    headers = {'User-Agent': 'Mozilla/5.0'}
    #for url in hotels:
    for i in range(counter):
        content=requests.get(("https://www.hotels.com/"+hotels[i]),headers=headers)
        soup=bs(content.content,'html.parser')
        filename = "HotelInfo\HotelNo_"+str(i)+".html"
        with open(filename, "w", encoding = 'utf-8') as file:
            file.write(str(soup.prettify()))
        time.sleep(5)
 

#read the individual files stores and extract information for each hotel
hotel_name=[]
Rating=[]
Reviews=[]
Amenities=[]
Address=[]
PlacesNearby=[]

def extract_hotel_details():
    for i in range(0,500):
        filename = "HotelInfo\HotelNo_"+str(i)+".html"
        with open(filename,encoding="utf8") as fp:
            soup = bs(fp, 'html.parser')
            try:
                data = soup.findAll('div',attrs={'data-stid':'content-hotel-title'})
                for j in data:
                    #hotel_name.append((j.find('h1')).text)
                    x = (j.find('h1')).text
                    hotel_name.append((x.strip()))
                    #x = re.search("([a-zA-z ,])", x)
            except:
                print('do nothing'+str(i))
                continue
            try:
                data = soup.findAll('div',attrs={'data-stid':'content-hotel-reviewsummary'})
                for j in data:
                    x=j.find('h3')
                    if(x is None):
                        Rating.append('')
                    else:
                        x = (j.find('h3')).getText()
                        Rating.append((x.strip()))
            except:
                print('do nothing'+str(i))
            try:
                data = soup.findAll('div',attrs={'data-stid':'content-hotel-reviewsummary'})
                for j in data:
                    x=j.find('h4')
                    if(x is None):
                        Reviews.append('')
                    else:
                        x = (j.find('h4')).get_text()
                        x = re.search('(\d+)', x)
                        review=int(x.group(1))
                        Reviews.append(review)
            except:
                print('do nothing'+str(i))
            #data = soup.findAll('div',attrs={'data-stid':'hotel-amenities-list'})
            try:
                data = soup.find('div',attrs={'data-stid':'hotel-amenities-list'}).findAll('li',attrs={'role':'listitem'})
                amen = []
                for j in data:
                    x=j.find('span')
                    #print(x.text())
                    if(x is None):
                        #Amenities.append([])
                        print('do nothing'+str(i))
                    else:
                        #x = (j.find('span')).getText()
                        x=j.find('span').get_text().strip()
                        #print(x)
                        amen.append(x)
                    #amen.append(j.find('span').text())
                Amenities.append(amen)
            except:
                #Amenities.append([])
                print('in except'+str(i))
            try:
                data = soup.findAll('div',attrs={'data-stid':'content-hotel-address'})
                for j in data:
                    if(j is None):
                        Address.append('')
                    else:
                        x=j.get_text().strip()
                        Address.append(x)
            except:
                #Amenities.append([])
                print('in except'+str(i))
            try:
                data1 = soup.findAll('span',attrs={'class':'uitk-layout-flex-item uitk-layout-flex-item-flex-grow-1'})
                places=[]
                for i in data1:
                    x=i.get_text().strip()
                    if(x == ''):
                       print('do nothing'+str(i))
                    else:
                        places.append(x)
                if(places == []):
                    print('do nothing')
                else:
                    PlacesNearby.append(places)
            except:
                #PlacesNearby.append([])
                print('do nothing'+str(i))

def get_collection():
    mongodb_client = MongoClient('mongodb+srv://vjmandekar:<password>@ddrcluster.vnnxiy3.mongodb.net/?retryWrites=true&w=majority')
    dbname = mongodb_client['SanFranciscoTravel']
    col = dbname['Hotels_SF']
    return col

def insert_into_hotel_collection():
    hotels = get_collection()
    for i in range(0,498):
        insert_dict={}
        insert_dict['HotelName'] = hotel_name[i]
        insert_dict['Address'] = Address[i]
        insert_dict['Ratings'] = Rating[i]
        insert_dict['NumberOfReviews'] = Reviews[i]
        insert_dict['Amenities'] = Amenities[i]        
        insert_dict['PlacesNearby'] = PlacesNearby[i]
        hotels.insert_one(insert_dict)
    #print(insert_dict)
    
def extract_geo_location():
    hotels_col = get_collection()
    base_url = "http://api.positionstack.com/v1/forward?access_key={}&query=".format("<accesskey>")
    hotels = hotels_col.find()
    for hotel_dict in hotels:
        try:
            address = hotel_dict['Address']
            url_gen = base_url + quote(address)
            for i in range(10):
                try:
                    geo_dict=requests.get(url_gen).json()['data'][0]
                    lat = geo_dict['latitude']
                    long = geo_dict['longitude']
                    hotels_col.update_one(
                        {'_id': hotel_dict['_id']},
                        {'$set': {'geo_location': {'Latitude': lat, 'Longitude': long}}}
                    )
    
                    time.sleep(5)
                    break
                except:
                    continue
                    time.sleep(5)
        except:
            continue


def show_viz():
    hotels_col = get_collection()
    df = pd.DataFrame(list(hotels_col.find()))
    df[['Latitude', 'Longitude','unwanted']] = df['geo_location'].apply(pd.Series)
    df.drop(columns='unwanted',inplace=True)
    fig = px.scatter_mapbox(df, 
                        lat="Latitude", 
                        lon="Longitude", 
                        hover_name="HotelName", 
                        hover_data=["Ratings","Address"],
                        zoom=8, 
                        height=800,
                        width=800)

    fig.update_layout(mapbox_style="open-street-map")
    fig.update_layout(margin={"r":0,"t":0,"l":0,"b":0})
    fig.write_html("HotelView.html")
    

            
            
if __name__ == '__main__':
    extract_hotel_details()    
    insert_into_hotel_collection()
    extract_geo_location()
    show_viz()
    







    
