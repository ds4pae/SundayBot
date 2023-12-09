package com.example.demo;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jsoup.nodes.Attribute;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.IOException;

import net.dv8tion.jda.api.entities.Message;

@SpringBootApplication
public class DemoApplication extends ListenerAdapter {
    private static final String TOKEN = "여기에 토큰 삽입 할 것";
    
    private static final String url = "https://maplestory.nexon.com/News/Event/Ongoing";
    private static final String nextPageUrl = "https://maplestory.nexon.com/News/Event/Closed";

    public static void main(String[] args) throws Exception {
        JDA jda = JDABuilder.createDefault(TOKEN)
                .setActivity(Activity.playing("개발"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new DemoApplication()) // 한 번만 추가
                .build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        if (message.startsWith("!썬데이")) {
            scrapeWebpage(event); // 스크래핑 함수를 호출
        }
        if (message.startsWith("일어나라 나의 하수인이여")) {
            answerToMaster(event); // 스크래핑 함수를 호출
        }

    }

    public void scrapeWebpage(MessageReceivedEvent event) {

        String keyword = "썬데이";
        String imageURL = "";

        StringBuilder result = new StringBuilder();
        boolean foundKeyword = false;

        // 첫 번째 페이지에서 키워드 찾기
        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");

            for (Element link : links) {
                String linkUrl = link.attr("abs:href");
                if (link.text().contains(keyword)) {
                    result.append("## 이번주 공지가 나왔어요.").append("\n");
                    result.append("### 이번주 공지 -> ");
                    result.append("[클릭](").append(linkUrl).append(")").append("\n");
                    imageURL = getPNGurl(linkUrl);
                    foundKeyword = true;
                    break;
                }
            }

            if (!foundKeyword) {
                result.append("## 아직 이번주 공지는 안나왔어요.").append("\n");
                result.append("### 저번주 공지 -> ");

            }
            // 첫 번째 페이지에서 키워드를 찾지 못한 경우 다음 페이지에서 키워드 찾기
            if (!foundKeyword) {
                doc = Jsoup.connect(nextPageUrl).get();
                links = doc.select("a[href]");

                for (Element link : links) {
                    String linkUrl = link.attr("abs:href");
                    if (link.text().contains(keyword)) {
                        imageURL = getPNGurl(linkUrl);
                        result.append("[클릭](").append(linkUrl).append(")").append("\n");
                        foundKeyword = true;

                        break;
                    }
                }
            }
        } catch (IOException e) {
            event.getChannel().sendMessage("Failed to fetch data from the URL: " + url).queue();
            return;
        }


        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("썬데이 알림")
                .setDescription(result.toString())
                .setColor(Color.GREEN);

       if (!imageURL.isEmpty()) {
            // 이미지가 있다면, 임베드에 이미지를 표시
            embedBuilder.setImage(imageURL);
            System.out.println("이미지 있음");
        } else if (imageURL.isEmpty()) {
            System.out.println("이미지 없음");
        }

        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public String getPNGurl(String linkUrl) {
        try {
            Document doc = Jsoup.connect(linkUrl).get();
            Element imgElement = doc.selectFirst("img[alt=썬데이 메이플]");

            if (imgElement != null) {
                String srcURL = imgElement.absUrl("src");
                return srcURL;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

}
