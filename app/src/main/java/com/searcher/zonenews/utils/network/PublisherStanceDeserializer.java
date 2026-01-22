package com.searcher.zonenews.utils.network;

import com.searcher.zonenews.entry.ArticleDetailEntry;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;

/**
 * Custom deserializer for PublisherStanceDTO to handle both string and object formats
 * from the API response for backward compatibility
 */
public class PublisherStanceDeserializer implements JsonDeserializer<ArticleDetailEntry.DataDTO.PublisherStanceDTO> {
    
    @Override
    public ArticleDetailEntry.DataDTO.PublisherStanceDTO deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {
        
        ArticleDetailEntry.DataDTO.PublisherStanceDTO stance = 
            new ArticleDetailEntry.DataDTO.PublisherStanceDTO();
        
        // Handle case where publisherStance is a string
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            String stanceString = json.getAsString();
            stance.setTag(stanceString);
            stance.setDisplayName(stanceString); // Use the string as display name if no object provided
            return stance;
        }
        
        // Handle case where publisherStance is an object
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            
            if (jsonObject.has("tag")) {
                stance.setTag(jsonObject.get("tag").getAsString());
            }
            
            if (jsonObject.has("displayName")) {
                stance.setDisplayName(jsonObject.get("displayName").getAsString());
            }
            
            return stance;
        }
        
        // Handle null case
        if (json.isJsonNull()) {
            return null;
        }
        
        // If it's neither string nor object, return null to indicate missing/invalid data
        return null;
    }
}

