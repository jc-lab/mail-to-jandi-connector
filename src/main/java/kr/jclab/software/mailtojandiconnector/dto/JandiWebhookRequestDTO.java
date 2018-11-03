package kr.jclab.software.mailtojandiconnector.dto;

import java.util.ArrayList;
import java.util.HashMap;

public class JandiWebhookRequestDTO {
    public String body;
    public String connectColor;
    public ArrayList<HashMap<String, Object>> connectInfo;
}
