package de.riedhammer.scraper;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import com.jsoniter.JsonIterator;

import lombok.ToString;

public class CoinmarketCap {


    private static String start = "20100101", end = "20191231";
    private static String url = "https://coinmarketcap.com/currencies/%s/historical-data/?start=%s&end=%s";
    private static String outfile = ".csv";


    public static void main(String[] args) throws Exception {
        for (Coin coin : coins()) {
            scrape(coin.id);
        }
    }

    static Coin[] coins() throws Exception {
        String data = IOUtils.toString(new URL("https://api.coinmarketcap.com/v1/ticker/?limit=0"));
        Coin[] coinList = JsonIterator.deserialize(data, Coin[].class);
        return coinList;

    }

    private static void scrape(String coinName) throws Exception {
        String dateFormatParse = "MMM dd, yyyy";
        DateFormat dfIn = new SimpleDateFormat(dateFormatParse, Locale.ENGLISH);
        String dateFormatGenerated = "yyyy-MM-dd";
        DateFormat dfOut = new SimpleDateFormat(dateFormatGenerated, Locale.ENGLISH);
        Document doc = null;
        try {
            doc = Jsoup.connect(String.format(url, coinName, start, end)).get();
        } catch (HttpStatusException ex) {
            if (ex.getStatusCode()==429)
                System.err.println("TOO MANY REQUESTS -> fehlerbehandlung & retry noch nicht gebaut");
            else if (ex.getStatusCode()==404)
                System.err.println("No Data for " + coinName + " in that time"); //offensichtlich hat cmc nicht für alles immer daten?!?
            else System.out.println(ex);
            return;
        }
        Elements hdr = doc.select("#historical-data > div > div.table-responsive > table > thead > tr");
        Element h = hdr.first();
        StringBuilder bh = new StringBuilder();
        for (Node n : h.getElementsByTag("th")) {
            if (bh.length() > 0) bh.append(",");
            bh.append(n.childNode(0).toString().toLowerCase());
        }

        Elements rows = doc.select("#historical-data > div > div.table-responsive > table > tbody > tr");
        Element r = rows.first();
        ArrayList<String> output = new ArrayList<String>();
        while ((r = r.nextElementSibling()) != null) {
            StringBuilder b = new StringBuilder();
            int i = 0;
            for (Node n : r.getElementsByTag("td")) {
                String mapped = n.childNode(0).toString();
                if (i++ == 0) {
                    try {
                        mapped = dfOut.format(new Date(dfIn.parse(mapped).getTime()));
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
                if (b.length() > 0) b.append(",");
                b.append(mapped.replaceAll(",", ""));
            }
            output.add(b.toString());
        }
        Collections.sort(output);
        PrintWriter writer = new PrintWriter(new FileWriter(coinName + outfile));
        writer.println(bh);
        for (String entry : output)
            writer.println(entry);
        writer.close();
        System.out.println("SUCCES Fetching data for: " + coinName);
    }

    @ToString
    public static class Coin {
        String id;    //": "bitcoin",
        String name;    //": "Bitcoin",
        String symbol;    //": "BTC",
        String rank;    //": "1",
        String price_usd;    //": "573.137",
        String price_btc;    //": "1.0",
        String _24h_volume_usd;    //": "72855700.0",
        String market_cap_usd;    //": "9080883500.0",
        String available_supply;    //": "15844176.0",
        String total_supply;    //": "15844176.0",
        String percent_change_1h;    //": "0.04",
        String percent_change_24h;    //": "-0.3",
        String percent_change_7d;    //": "-0.57",
        String last_updated;    //": "1472762067"
    }

}
