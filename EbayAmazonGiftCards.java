package msba23;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EbayAmazonGiftCards {
	
	public static void GetFiles(int no_of_pages) throws InterruptedException {
		try {
				
				String url = "https://www.ebay.com/sch/i.html?_from=R40&_nkw=amazon+gift+card&LH_Sold=1";
				for(int i=1;i<=no_of_pages;i++)
				{
					TimeUnit.SECONDS.sleep(10);
					URL book_url = new URL(url);
					url = url + "_pgn=" + i;
			        BufferedReader readr = 
			          new BufferedReader(new InputStreamReader(book_url.openStream()));
			        String file_no = "";
			        
			        if(i<10) {
			        	file_no = "0"+i;
			        }else {
			        	file_no= Integer. toString(i);
			        }
			        String filename = "EbayAmazon/amazon_gift_card_"+file_no+".htm";
			        BufferedWriter writer = 
			          new BufferedWriter(new FileWriter(filename));
			        String line;
			        while ((line = readr.readLine()) != null) {
			            writer.write(line);
			        }
			        readr.close();
			        writer.close();
				}
				
		        
			}
			catch (MalformedURLException mue) {
		        System.out.println("Malformed URL Exception raised");
		    }
		    catch (IOException ie) {
		        System.out.println("IOException raised");
		    }
		}

	//This function parses the html file and pulls the title, sell price and shipping price for 
	//each product
		public static void ParseFiles(String file_name, List<String> list_of_Title, List<String> list_of_prices, List<String> list_of_ship ) throws IOException{
				File input = new File(file_name);
				Document doc = Jsoup.parse(input, null);
				Elements ul = doc.select("div.srp-river-results > ul");
				Elements li = ul.select("li");
				Elements items_per_page = doc.select("div.srp-ipp span.btn__cell");
				int ipp = Integer.parseInt(items_per_page.text());
				
				for(int i = 1;i<=li.size()-1;i++)
				{
					//Here we look for all <li> tags that have the attribute data-view to get the details 
					//for each product
					String is_data_view = li.get(i).attr("data-view");
					if(is_data_view.equals(null) || is_data_view.equals("")) {
						continue;
					}
					Elements item_title = li.get(i).select("div.s-item__title");
					String Title = item_title.text();
					list_of_Title.add(Title);
					Elements item_price = li.get(i).select("div.s-item__details div.s-item__detail span.s-item__price");
					String Price = item_price.text();
					list_of_prices.add(Price);
					Elements shipping_price = li.get(i).select("div.s-item__details div.s-item__detail span.s-item__shipping");
					String ShippingPrice = shipping_price.text();
					list_of_ship.add(ShippingPrice);
				}		
				
		}

		//This function checks for a range of selling prices. For eg. $25 to $150. Since for these orders
		//we wont be able to compare the face value to true value, we make the price 0.00
		public static List<String> CheckforPriceRange(List<String> list_toExtract) throws IOException{
			List<String> sell_prices = new ArrayList<String>();
			for(int i=0;i<list_toExtract.size();i++)
			{
				Pattern p = Pattern.compile(".*?\\$([0-9]{1,3}[,.]?[0-9]{0,2}\s[t][o]\s\\$[0-9]{1,3}[,.]?[0-9]{0,2}).*?");
				Matcher m = p.matcher(list_toExtract.get(i));
		    if (m.find())
		    {
		      sell_prices.add("$0.00");
		    }else {
		    	sell_prices.add(list_toExtract.get(i));
		    }
		    
			}
			return sell_prices;
		}

		//This function extracts only the dollar value from the string
		public static List<String> ExtractDetails(List<String> list_toExtract) throws IOException{
			List<String> sell_prices = new ArrayList<String>();
			for(int i=0;i<list_toExtract.size();i++)
			{
				Pattern p = Pattern.compile(".*?\\$([0-9]{1,3}[,.]?[0-9]{0,2}).*?");
				//Pattern p = Pattern.compile(".*?\\$([0-9]{1,3}[,.]?[0-9]{0,2}\s[t][o]\s\\$[0-9]{1,3}[,.]?[0-9]{0,2}).*?");
				Matcher m = p.matcher(list_toExtract.get(i));
				String sell_price=null;
		    if (m.find())
		    {
		      sell_price = m.group(1);
		      //sell_prices.add(Integer.parseInt(sell_price));
		      sell_prices.add(sell_price);
		    }else {
		    	sell_prices.add("0.00");
		    }
		    
			}
			return sell_prices;
		}

		//This function is used to remove "New Listing" from the title
		public static List<String> ExtractTitle(List<String> list_toExtract) throws IOException{
			List<String> product_title = new ArrayList<String>();
			for(int i=0;i<list_toExtract.size();i++)
			{
				Pattern p = Pattern.compile("(New Listing)(.*)");
				//Pattern p = Pattern.compile(".*?\\$([0-9]{1,3}[,.]?[0-9]{0,2}\s[t][o]\s\\$[0-9]{1,3}[,.]?[0-9]{0,2}).*?");
				Matcher m = p.matcher(list_toExtract.get(i));
				String title=null;
		    if (m.find())
		    {
		    	title = m.group(2);
		      //sell_prices.add(Integer.parseInt(sell_price));
		    	product_title.add(title);
		    }else {
		    	product_title.add(list_toExtract.get(i));
		    }
		    
			}
			return product_title;
		}


		//once we have prices from the title and true price (sell price+ship price) we compare the 
		//two prices to see if products were sold above the face value
		public static float ComparePrices(List<String> TitlePrice, List<String> SellPrice, List<String> ShipPrice)throws IOException{
			int more_than_face = 0;
			int null_count = 0;
			Float total_value = (float) 0.00;
			for(int i=0;i<TitlePrice.size();i++) {
				if(Float.parseFloat(SellPrice.get(i)) == 0.00) {
					total_value = (float) 0.00;
				}else {
					total_value = Float.parseFloat(SellPrice.get(i))+Float.parseFloat(ShipPrice.get(i));
				}
				
				Float face_value = Float.parseFloat(TitlePrice.get(i));
				if(face_value<total_value && total_value != 0.00) {
					more_than_face++;
				}
				if(total_value==0.00) {
					null_count++;
				}
			}
			float more_than_face_prob = (float) more_than_face/((SellPrice.size()-null_count));
			return more_than_face_prob;
		}

		//This function accesses the fctables website and login into my account. It also saves the 
		//cookies from the session
		public static Map<String,String> GetFCTablesCookies() throws IOException, InterruptedException {
			
			Response res = Jsoup.connect("https://www.fctables.com/")
					                            .userAgent("Mozilla/5.0")
					                            .data("user_remeber","1")
					                            .data("login_username","vindhyamandekar")
					                            .data("login_password","abcdefgh")
					                            .data("login_action","1")
					                            .data("submit","submit")
					                            .method(Method.POST)
					                            .execute();
					        Map<String,String> cookies = res.cookies();
					        return cookies;
			
		}

		public static void main(String[] args) throws IOException, InterruptedException {
			//Call get files to download first ten product pages 
			GetFiles(10);
			List<String> list_of_Title = new ArrayList<String>();
			List<String> list_of_prices = new ArrayList<String>();
			List<String> list_of_ship = new ArrayList<String>();
			String file_no = null;
			//For each of these pages we parse them one by one to extract the details and store them in lists
			for(int i = 1;i<=10;i++) {
				if(i<10) {
		        	file_no = "0"+i;
		        }else {
		        	file_no= Integer. toString(i);
		        }
				ParseFiles("C:/Users/vindh/eclipse-workspace/MSBAWebScrapping/EbayAmazon/amazon_gift_card_"+file_no+".htm",
						list_of_Title,list_of_prices,list_of_ship);
			}
			List<String> Title_Price = new ArrayList<String>();
			list_of_Title = ExtractTitle(list_of_Title);
			System.out.println("The list of titles are "+list_of_Title);
			Title_Price = ExtractDetails(list_of_Title);
			System.out.println("The list of Title Prices are "+Title_Price);
			List<String> Sell_Price = new ArrayList<String>();
			Sell_Price = CheckforPriceRange(list_of_prices);
			Sell_Price = ExtractDetails(Sell_Price);
			System.out.println("The list of Selling prices are " +Sell_Price);
			List<String> Ship_Price = new ArrayList<String>();
			Ship_Price = ExtractDetails(list_of_ship);
			System.out.println("The list of Shipping prices are "+Ship_Price);
			float prob_more_than_face = ComparePrices(Title_Price,Sell_Price,Ship_Price);
			System.out.println("The proportion of items that are sold above face value is " + prob_more_than_face);
			System.out.println("The reason that around 63 percent of the products are sold above face level is provided in the text file attached with the assignment");
			
		}

}
