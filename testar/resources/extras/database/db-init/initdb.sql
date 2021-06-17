USE testar;

DROP TABLE IF EXISTS `reports`;
CREATE TABLE `reports` (
    `id` int NOT NULL AUTO_INCREMENT,
    `tag` varchar(255),
    `time` datetime NOT NULL,
    `actions_per_sequence` int,
    `total_sequences` int,
    `url` varchar(255),
    PRIMARY KEY (`id`)
);

DROP TABLE IF EXISTS `iterations`;
CREATE TABLE `iterations` (
    `id` int NOT NULL AUTO_INCREMENT,
    `report_id` int,
    `info` varchar(255),
    `severity` double,
    `last_executed_action_id` int,
    `last_state_id` int,
    PRIMARY KEY (`id`)
    -- FOREIGN KEY (`report_id`) REFERENCES `reports`(`id`)
);

DROP TABLE IF EXISTS `actions`;
CREATE TABLE `actions` (
    `id` int NOT NULL AUTO_INCREMENT,
    `iteration_id` int,
    `description` varchar(255),
    `name` varchar(255),
    `status` varchar(255),
    `screenshot` varchar(255),
    `start_time` datetime,
    PRIMARY KEY (`id`)
    -- FOREIGN KEY(`iteration_id`) REFERENCES `iterations` (`id`)
);

DROP TABLE IF EXISTS `sequence_items`;
CREATE TABLE `sequence_items` (
    `id` int NOT NULL AUTO_INCREMENT,
    `concrete_id` varchar(255),
--    `cidcustom` varchar(255),
    `abstract_id` varchar(255),
    `abstract_r_id` varchar(255),
    `abstract_r_t_id` varchar(255),
    `abstract_r_t_p_id` varchar(255),
    PRIMARY KEY (`id`)
);

-- ALTER TABLE `iterations` ADD FOREIGN KEY (`last_executed_action_id`) REFERENCES `actions`(`id`);
-- ALTER TABLE `iterations` ADD FOREIGN KEY (`last_state_id`) REFERENCES `sequence_items`(`id`);
