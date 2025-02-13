package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon;

public interface SqlConstants {
    String TABLE_NAME_PLAYER_PROFILE = "player_profile";
    String TABLE_NAME_PLAYER_RESEARCH = "player_research";
    String TABLE_NAME_BACKPACK = "player_backpack";
    String TABLE_NAME_BACKPACK_INVENTORY = "backpack_inventory";
    String TABLE_NAME_BLOCK_RECORD = "block_record";
    String TABLE_NAME_BLOCK_DATA = "block_data";
    String TABLE_NAME_CHUNK_DATA = "chunk_data";
    String TABLE_NAME_BLOCK_INVENTORY = "block_inventory";
    String TABLE_NAME_UNIVERSAL_INVENTORY = "universal_inventory";
    String TABLE_NAME_UNIVERSAL_RECORD = "universal_record";
    String TABLE_NAME_UNIVERSAL_DATA = "universal_data";
    /**
     * @deprecated
     * 由于设计不当，该表已被弃用
     */
    String TABLE_NAME_TABLE_INFORMATION = "table_information";

    String TABLE_NAME_TABLE_METADATA = "table_metadata";

    String FIELD_PLAYER_UUID = "p_uuid";
    String FIELD_PLAYER_NAME = "p_name";

    String FIELD_RESEARCH_KEY = "research_id";

    String FIELD_BACKPACK_ID = "b_id";
    String FIELD_BACKPACK_SIZE = "b_size";
    String FIELD_BACKPACK_NAME = "b_name";
    String FIELD_BACKPACK_NUM = "b_num";

    String FIELD_INVENTORY_SLOT = "i_slot";
    String FIELD_INVENTORY_ITEM = "i_item";

    String FIELD_LOCATION = "loc";
    String FIELD_CHUNK = "chunk";
    String FIELD_SLIMEFUN_ID = "sf_id";

    String FIELD_DATA_KEY = "data_key";
    String FIELD_DATA_VALUE = "data_val";

    String FIELD_UNIVERSAL_UUID = "universal_uuid";

    String FIELD_UNIVERSAL_TRAITS = "universal_traits";

    /**
     * @deprecated
     * 由于设计不当，该字段已被弃用
     */
    String FIELD_TABLE_VERSION = "table_version";

    String FIELD_TABLE_METADATA_KEY = "table_metadata_key";
    String FIELD_TABLE_METADATA_VALUE = "table_metadata_value";

    String METADATA_VERSION = "version";
}
