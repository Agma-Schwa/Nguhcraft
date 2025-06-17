#include <base/Base.hh>
#include <base/Text.hh>
#include <clopts.hh>
#include <nlohmann/json.hpp>
#include <print>

using namespace base;
using nlohmann::json;

// Escape a UTF-16 sequence as JSON hex codes.
auto Escape(std::u16string_view text) -> std::string {
    return utils::join(text, "", "\\u{:04x}", [](char16_t c) { return u16(c); });
}

// Remap U+f0000.. to U+f1000..
char32_t largest = 0;
auto Remap(const json& data) -> std::string {
    auto s = text::ToUTF32(data.get<std::string>());
    for (auto& c : s) {
        if (c >= 0xf0000) c += 0x1000;
        largest = std::max(largest, c);
    }
    return Escape(text::ToUTF16(s));
}

// Generate the replacement list for multi-codepoint sequences.
void RemapCodepointReplacements(const json& data) {
    Assert(data.is_object());

    std::println("{{");
    for (const auto& [seq_8, repl] : data.get<json::object_t>()) {
        auto seq = text::ToUTF16(seq_8);
        std::println("    \"{}\": \"{}\",", Escape(seq), Remap(repl));
    }
    std::println("}}");
}


// Remap the font map in font/defaults.json
void RemapFontMap(const json& data) {
    Assert(data.is_array());
    auto font_map = data.get<json::array_t>();

    std::println("[");
    for (const auto& row : font_map) std::println("    \"{}\",", Remap(row));
    std::println("]");
}

// Remap the ':named:' replacements.
void RemapNamedReplacements(const json& data) {
    Assert(data.is_object());
    std::println("{{");
    for (const auto& [name, repl] : data.get<json::object_t>())
        std::println("    \"{}\": \"{}\",", name.substr(1, name.size() - 2), Remap(repl));
    std::println("}}");
}

int main(int argc, char** argv) {
    using namespace command_line_options;
    using options = clopts<
        positional<"action", "action to perform", values<"font", "codepoints", "named">>,
        positional<"file", "input file", file<>>, help<>
    >;

    auto opts = options::parse(argc, argv);
    auto data = json::parse(opts.get<"file">()->contents);
    auto a = *opts.get<"action">();
    if (a == "font") RemapFontMap(data);
    else if (a == "codepoints") RemapCodepointReplacements(data);
    else if (a == "named") RemapNamedReplacements(data);
    else Unreachable();
    std::println(stderr, "Largest codepoint in use: U+{:x}", largest);
}
